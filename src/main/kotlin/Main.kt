/*
Copyright 2023 Chris Basinger

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/



import java.util.SortedSet
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ThreadPoolExecutor

fun main(args: Array<String>) {
    println("Find the greatest common factor by entering a list of number seperated by a space")
    loop@while(true){
        print("Enter your numbers: ")
        val input = readlnOrNull()?.trim()
        if(input.isNullOrBlank())
            continue@loop
        if(input.lowercase() == "exit")
            break
        else{
            try{
                val numbers = input.split(" ").map{ it.toLongOrNull() }.requireNoNulls().toSortedSet()
                val result = gcf(numbers)
                println("GCF: ${result.gcf}")
                println("Common Factors: ${result.commonFactors.joinToString(" ")}")
                result.factors.toSortedMap().forEach {
                    print("Factors for ${it.key}: ")
                    print(it.value.sorted().joinToString(" "))
                    println()
                }
            }catch (e: NegativeNumberException){
                println(e.message)
            }catch (e: NumberZeroException){
                println(e.message)
            }catch(e: IllegalArgumentException){
                println("Please enter real numbers with no text")
            }

        }
    }
}

@Throws(NumberZeroException::class, NegativeNumberException::class)
fun gcf(numbers: SortedSet<Long>): FactorResult<Long> {
    numbers.forEach {
        if(it == 0L) throw NumberZeroException()
        if(it < 0L) throw NegativeNumberException()
    }

    val factors = ConcurrentHashMap<Long, MutableList<Long>>()
    numbers.forEach { num ->
        factors[num] = mutableListOf()
    }

    var pool = Executors.newFixedThreadPool(numbers.size) as ThreadPoolExecutor

    val tasks = mutableListOf<Future<*>>()
    numbers.forEach { num ->
       val task = pool.submit {
            for (i in 1..num) {
                if (num % i == 0L) {
                    synchronized(factors){
                        factors[num]!!.add(i)
                    }
                }
            }
        }
        tasks.add(task)
    }
    tasks.forEach { it.get() }

    if (numbers.size == 1) {
        factors[numbers.first()]!!
        return FactorResult(factors[numbers.first()]!!.last(), factors[numbers.first()]!!, factors)
    }

    val commonFactors = Collections.synchronizedList(mutableListOf<Long>())
    numbers.drop(1)
    tasks.clear()
    if(pool.maximumPoolSize < factors[factors.keys.first()]!!.size)
        pool.maximumPoolSize = factors[factors.keys.first()]!!.size
    factors[factors.keys.first()]!!.forEach { factor ->
        val task = pool.submit {
            var count = 0
            numbers.forEach { num ->
                if (factors[num]!!.contains(factor))
                    count++
            }
            if (count == numbers.size)
                synchronized(commonFactors){
                    commonFactors.add(factor)
                }
        }
        tasks.add(task)
    }

    tasks.forEach { it.get() }
    val result = FactorResult<Long>(commonFactors.max(), commonFactors, factors)
    return result
}

class FactorResult<T: Number>(val gcf: T, val commonFactors: List<T>, val factors: Map<T,List<T>>)

class NumberZeroException: Exception("Do not enter the number zero")
class NegativeNumberException: Exception("Do not enter negative numbers")