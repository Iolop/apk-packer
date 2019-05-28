package com.android.dump

import android.util.Log
import java.lang.Exception
import java.lang.reflect.Field

fun invoke_static_method(className: String,
                         methodName: String,
                         pareTypes: Array<Class<*>>,
                         pareValues: Array<Any>): Any?{
    try {
        val objClass = Class.forName(className)
        val method = objClass.getMethod(methodName,*pareTypes)
        Log.d("dump",method.toString())
        method.isAccessible = true
        return method.invoke(null,*pareValues)
    }catch (e: Exception){
        Log.d("dump",e.toString())
        e.printStackTrace()
    }
    return null
}

fun invoke_method(className: String,
                  methodName: String,
                  obj: Any?,
                  pareTypes: Array<Class<*>>,
                  pareValues: Array<Any?>): Any?{
    try {
        val objClass: Class<*> = Class.forName(className)
        val method = objClass.getMethod(methodName,*pareTypes)
        return method.invoke(obj,*pareValues)
    }catch (e: Exception){
        Log.d("dump",e.toString())
        e.printStackTrace()
    }
    return null
}

fun get_field_object(className: String,
                     obj: Any?,
                     fieldName: String): Any?{
    try {
        val objClass = Class.forName(className)
        val field = objClass.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.get(obj)
    }catch (e: Exception){
        Log.d("dump",e.toString())
        e.printStackTrace()
    }
    return null
}

fun get_static_field_object(className: String,
                            fieldName: String): Any?{
    try {
        val objClass = Class.forName(className)
        val field = objClass.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.get(null)
    }catch (e: Exception){
        Log.d("dump",e.toString())
        e.printStackTrace()
    }
    return null
}

fun set_field_object(className: String,
                     fieldName: String,
                     obj: Any?,
                     objVaule: Any?): Any?{
    try {
        val objClass = Class.forName(className)
        val field = objClass.getDeclaredField(fieldName)
        field.isAccessible = true
        Log.d("dump","obj "+obj.toString()+" objValue "+objVaule.toString())
        return field.set(obj,objVaule)
    }catch (e: Exception){
        Log.d("dump",e.toString())
        e.printStackTrace()
    }
    return null
}

fun set_static_field_object(className: String,
                     fieldName: String,
                     objVaule: Any): Any?{
    try {
        val objClass = Class.forName(className)
        val field = objClass.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.set(null,objVaule)
    }catch (e: Exception){
        Log.d("dump",e.toString())
        e.printStackTrace()
    }
    return null
}