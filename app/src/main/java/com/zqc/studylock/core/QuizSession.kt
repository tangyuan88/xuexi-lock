package com.zqc.studylock.core

import com.zqc.studylock.data.Question
import com.zqc.studylock.data.QuestionBank
import com.zqc.studylock.data.WrongBook
import kotlin.math.max

/**
 * 答题会话：一次解锁需要连续答对 N 道题。
 * 规则：答错立即记入错题本并重置当前进度；答对推进；错题本中的题会周期性插入复习。
 */
class QuizSession(
    private val bank: QuestionBank,
    private val wrongBook: WrongBook,
    private val total: Int
) {
    private var queue: MutableList<Question> = mutableListOf()
    private var index = 0
    var solved = 0
        private set
    var wrongCount = 0
        private set

    /** 初始化抽题：优先从错题本加权抽取，其余从启用题库随机补齐 */
    fun start() {
        val wanted = max(total, 1)
        val review = wrongBook.weighted(wanted / 3)
        val pool = bank.allEnabledQuestions().filter { q -> review.none { it.stem == q.stem } }
        queue = (review + pool.shuffled()).distinctBy { it.stem }.take(wanted).toMutableList()
        if (queue.isEmpty()) {
            // 题库为空的兜底题，保证流程可用
            queue.add(
                Question(
                    stem = "题库为空：请在家长控制端导入题库包后再试。",
                    options = listOf("我知道了", "我知道了", "我知道了", "我知道了"),
                    answer = 0
                )
            )
        }
        index = 0; solved = 0; wrongCount = 0
    }

    fun current(): Question? = queue.getOrNull(index)

    /** 提交答案，返回是否需要换题。true 表示本题结束（无论对错）。 */
    fun answer(choice: Int): Boolean {
        val q = current() ?: return false
        if (choice == q.answer) {
            solved++
            wrongBook.addRight(q)
        } else {
            wrongCount++
            solved = 0            // 答错重置连对
            wrongBook.addWrong(q)
            queue.add(q)          // 答错的题重新排入队尾，直到答对为止
        }
        index++
        return true
    }

    val finished: Boolean get() = solved >= total
    val progress: Pair<Int, Int> get() = solved to total
}
