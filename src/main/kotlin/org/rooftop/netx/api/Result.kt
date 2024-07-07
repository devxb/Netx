package org.rooftop.netx.api

import org.rooftop.netx.core.Codec
import kotlin.reflect.KClass

/**
 * Returns the result of a Saga.
 */
class Result<T : Any> private constructor(
    /**
     * Indicates whether the Saga was successful.
     */
    val isSuccess: Boolean,
    private val codec: Codec,
    private val result: String?,
    private val error: Error? = null,
) {

    /**
     * If failed, throws the exception that caused the failure; if successful, returns the result.
     *
     * @param typeReference
     * @return T result of saga
     */
    fun decodeResultOrThrow(typeReference: TypeReference<T>): T {
        if (!isSuccess) {
            throwError()
        }
        return decodeResult(typeReference)
    }

    /**
     * @see decodeResultOrThrow
     */
    fun decodeResultOrThrow(type: Class<T>): T = decodeResultOrThrow(type.kotlin)

    /**
     * @see decodeResultOrThrow
     */
    fun decodeResultOrThrow(type: KClass<T>): T {
        if (!isSuccess) {
            throwError()
        }
        return decodeResult(type)
    }

    /**
     * If successful, returns the result.
     *
     * If the Result cannot be found, throws a ResultException.
     *
     * @param typeReference
     * @return T result of saga
     * @throws ResultException
     */
    fun decodeResult(typeReference: TypeReference<T>): T = result?.let {
        codec.decode(it, typeReference)
    } ?: throw ResultException("Cannot decode result cause Result is fail state")

    /**
     * @see decodeResult
     */
    fun decodeResult(type: Class<T>): T = decodeResult(type.kotlin)

    /**
     * @see decodeResult
     */
    fun decodeResult(type: KClass<T>): T = result?.let {
        codec.decode(it, type)
    } ?: throw ResultException("Cannot decode result cause Result is fail state")

    /**
     * Throws an exception if failed.
     *
     * If the exception cannot be found, throws a ResultException.
     * @throws ResultException
     */
    fun throwError() = error?.throwError(codec)
        ?: throw ResultException("Cannot throw error cause Result is success state")

    override fun toString(): String {
        return "Result(isSuccess=$isSuccess, codec=$codec, result=$result, error=$error)"
    }

    private class Error(
        private val error: String,
        private val type: KClass<Throwable>
    ) {

        fun throwError(codec: Codec) {
            throw codec.decode(error, type)
        }

        override fun toString(): String {
            return "Error(error='$error', type=$type)"
        }
    }

    internal companion object {

        fun <T : Any> success(
            codec: Codec,
            result: String,
        ): Result<T> {
            return Result(
                true,
                codec,
                result,
                null,
            )
        }

        fun <T : Any> fail(
            codec: Codec,
            error: String,
            type: KClass<Throwable>,
        ): Result<T> {
            return Result(
                false,
                codec,
                null,
                Error(error, type),
            )
        }
    }
}
