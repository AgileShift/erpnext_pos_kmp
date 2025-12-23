package com.erpnext.pos.domain.usecases

/**
 * Base UseCase pattern
 * Generic input [I] and output [O].
 */
abstract class UseCase<I, O> {
    suspend operator fun invoke(input: I): O = useCaseFunction(input)
    protected abstract suspend fun useCaseFunction(input: I): O
}
