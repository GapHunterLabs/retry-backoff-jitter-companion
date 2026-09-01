package dev.gaphunter.retrybackoffjittercompanion.detect

import com.intellij.psi.JavaRecursiveElementWalkingVisitor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiCatchSection
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiLoopStatement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiTryStatement
import com.intellij.psi.util.PsiTreeUtil
import dev.gaphunter.retrybackoffjittercompanion.model.RetryBackoffHit
import dev.gaphunter.retrybackoffjittercompanion.model.RetryPatternKind

/**
 * Finds three known retry shapes with a FIXED interval instead of
 * exponential backoff + jitter -- the real "thundering herd" anti-pattern:
 * synchronized retries from many instances of a service saturate the
 * downstream exactly at the same instant, documented in distributed-
 * systems literature (AWS Architecture Blog, Google SRE Book) as a real
 * cause of cascading availability incidents.
 *
 * **v0.1 scope, stated honestly:** Spring Retry and Resilience4j (known
 * declarative forms) plus the manual `for`/`while` + `catch` +
 * `Thread.sleep(constant)` pattern -- never analyzes a fully custom
 * retry implementation with none of these recognizable signals.
 */
object JavaRetryBackoffFinder {

    fun findAll(file: PsiFile): List<RetryBackoffHit> {
        val hits = mutableListOf<RetryBackoffHit>()
        file.accept(object : JavaRecursiveElementWalkingVisitor() {
            override fun visitTryStatement(statement: PsiTryStatement) {
                super.visitTryStatement(statement)
                hitForManualLoop(statement)?.let { hits += it }
            }

            override fun visitMethod(method: PsiMethod) {
                super.visitMethod(method)
                hitForSpringRetryable(method)?.let { hits += it }
            }

            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                super.visitMethodCallExpression(expression)
                hitForResilience4j(expression)?.let { hits += it }
            }
        })
        return hits
    }

    /** A `try` lexically inside a loop whose `catch` calls `Thread.sleep(<constant literal>)`. */
    private fun hitForManualLoop(tryStatement: PsiTryStatement): RetryBackoffHit? {
        val enclosingLoop = PsiTreeUtil.getParentOfType(tryStatement, PsiLoopStatement::class.java, true, PsiMethod::class.java)
        if (enclosingLoop == null) return null

        for (catchSection: PsiCatchSection in tryStatement.catchSections) {
            val sleepCall = findSleepCall(catchSection) ?: continue
            return RetryBackoffHit(anchorOf(sleepCall.methodExpression), RetryPatternKind.MANUAL_LOOP)
        }
        return null
    }

    private fun findSleepCall(catchSection: PsiCatchSection): PsiMethodCallExpression? {
        val body = catchSection.catchBlock ?: return null
        var found: PsiMethodCallExpression? = null
        body.accept(object : JavaRecursiveElementWalkingVisitor() {
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                if (found != null) return
                super.visitMethodCallExpression(expression)
                val methodExpr = expression.methodExpression
                if (methodExpr.referenceName != "sleep") return
                if (methodExpr.qualifierExpression?.text != "Thread") return
                val arg = expression.argumentList.expressions.getOrNull(0) ?: return
                if (arg !is PsiLiteralExpression) return // a computed/variable delay isn't the constant-interval shape this checks for
                found = expression
            }
        })
        return found
    }

    /** `@Retryable` with no `@Backoff` at all, or one with no real `multiplier` (> 1). */
    private fun hitForSpringRetryable(method: PsiMethod): RetryBackoffHit? {
        val retryable = method.annotations.firstOrNull { it.nameReferenceElement?.referenceName == "Retryable" } ?: return null
        val anchor = retryable.nameReferenceElement ?: retryable
        val backoffValue = retryable.findAttributeValue("backoff") as? PsiAnnotation

        if (backoffValue == null) {
            // No @Backoff at all -- Spring's own default is an IMMEDIATE
            // retry with zero delay, worse than even a fixed interval.
            return RetryBackoffHit(anchor, RetryPatternKind.SPRING_RETRYABLE)
        }

        val multiplier = (backoffValue.findAttributeValue("multiplier") as? PsiLiteralExpression)?.value as? Number
        if (multiplier == null || multiplier.toDouble() <= 1.0) {
            return RetryBackoffHit(anchor, RetryPatternKind.SPRING_RETRYABLE)
        }
        return null
    }

    /** `RetryConfig.custom()....waitDuration(...)...build()` with no `.intervalFunction(...)` anywhere in the same chain. */
    private fun hitForResilience4j(call: PsiMethodCallExpression): RetryBackoffHit? {
        if (!isTerminalCallInChain(call)) return null
        if (call.methodExpression.referenceName != "build") return null

        var current: PsiElement? = call.methodExpression.qualifierExpression
        var sawWaitDuration = false
        var sawIntervalFunction = false
        while (current is PsiMethodCallExpression) {
            when (current.methodExpression.referenceName) {
                "waitDuration" -> sawWaitDuration = true
                "intervalFunction" -> sawIntervalFunction = true
                "custom" -> {
                    val qualifier = current.methodExpression.qualifierExpression ?: return null
                    if (qualifier.text != "RetryConfig") return null
                    if (!sawWaitDuration || sawIntervalFunction) return null
                    return RetryBackoffHit(anchorOf(call.methodExpression), RetryPatternKind.RESILIENCE4J)
                }
            }
            current = current.methodExpression.qualifierExpression
        }
        return null
    }

    private fun isTerminalCallInChain(call: PsiMethodCallExpression): Boolean {
        val parent = call.parent
        return !(parent is PsiReferenceExpression && parent.qualifierExpression === call)
    }

    private fun anchorOf(methodExpr: PsiReferenceExpression): PsiElement = methodExpr.referenceNameElement ?: methodExpr
}
