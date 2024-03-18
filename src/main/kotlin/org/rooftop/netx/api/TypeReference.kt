package org.rooftop.netx.api

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

abstract class TypeReference<T> private constructor() {
    val type: Type

    init {
        val superClass = javaClass.genericSuperclass
        type = (superClass as ParameterizedType).actualTypeArguments[0]
    }

    companion object {

        fun <T> from(type: T): TypeReference<T> = object : TypeReference<T>() {}
    }
}
