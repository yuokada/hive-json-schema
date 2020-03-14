package io.github.yuokada

import java.io.File

fun fizzbuzz(i: Int): Unit {
    when {
        i % 15 == 0 -> println("fizzbuzz")
        i % 5 == 0 -> println("buzz")
        i % 3 == 0 -> println("fizz")
        else -> println(i)
    }
}

fun main(args: Array<String>) {
    println("Hello World")

    val file = File("etc/sample.json")
    for (ln in file.readLines()) {
        println(ln)
    }

    val p = intArrayOf(1,2,3,4,5)
    val q  = arrayOf(10,20, "foo")
    p.contains(2)

    val r = IntRange(1, 30)
    r.forEach { fizzbuzz(it) }
    IntRange(0, 20).forEach { print("=") }
    r.map {
        when {
            it % 15 == 0 -> "fizzbuzz"
            it % 5 == 0 -> "buzz"
            it % 3 == 0 -> "fizz"
            else -> it.toString()
        }
    }.forEach { println(it) }
}