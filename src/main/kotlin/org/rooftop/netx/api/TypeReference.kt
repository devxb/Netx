package org.rooftop.netx.api

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * Maintains full-generics-type until runtime by subclassing.
 *
 * Example.
 *
 *      val typeReference = object: TypeReference<Pair<Foo, Foo>>(){ }
 */
abstract class TypeReference<T : Any> {
    val type: Type

    init {
        val superClass: Type = this.javaClass.genericSuperclass
        type = (superClass as ParameterizedType).actualTypeArguments[0]
    }
}
