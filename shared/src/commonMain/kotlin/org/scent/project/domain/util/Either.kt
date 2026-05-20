package org.scent.project.domain.util

import org.scent.project.domain.error.AppError

sealed class Either<out L, out R> {
    data class Left<out L>(val value: L) : Either<L, Nothing>()
    data class Right<out R>(val value: R) : Either<Nothing, R>()

    val isRight get() = this is Right<R>
    val isLeft  get() = this is Left<L>

    inline fun <C> fold(ifLeft: (L) -> C, ifRight: (R) -> C): C = when (this) {
        is Left  -> ifLeft(value)
        is Right -> ifRight(value)
    }
    inline fun <C> map(f: (R) -> C): Either<L, C> = when (this) {
        is Left  -> Left(value)
        is Right -> Right(f(value))
    }
    inline fun <C> flatMap(f: (R) -> Either<L, C>): Either<L, C> = when (this) {
        is Left  -> Left(value)
        is Right -> f(value)
    }
    inline fun onRight(action: (R) -> Unit): Either<L, R> { if (this is Right) action(value); return this }
    inline fun onLeft(action: (L) -> Unit): Either<L, R>  { if (this is Left)  action(value); return this }
    fun getOrNull(): R? = if (this is Right) value else null
    fun leftOrNull(): L? = if (this is Left) value else null
}

fun <R> R.asRight(): Either<Nothing, R> = Either.Right(this)
fun <L> L.asLeft(): Either<L, Nothing>  = Either.Left(this)

typealias Result<T> = Either<AppError, T>
