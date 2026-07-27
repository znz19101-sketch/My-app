
package com.guardexa.core.time

data class TimePolicy(
    val dailyLimitMinutes:Int,
    val graceSeconds:Int,
    val bonusMinutes:Int=0
)

data class SessionState(
    val usedMinutes:Int,
    val graceRemaining:Int,
    val active:Boolean
)

class UsageEngine{
    fun remaining(policy:TimePolicy,used:Int):Int=
        (policy.dailyLimitMinutes+policy.bonusMinutes-used).coerceAtLeast(0)
}

class GraceEngine{
    fun isExpired(seconds:Int)=seconds<=0
    fun tick(seconds:Int)=if(seconds>0) seconds-1 else 0
}

class TimeEngine(
    private val usageEngine:UsageEngine=UsageEngine(),
    private val graceEngine:GraceEngine=GraceEngine()
){
    fun buildState(policy:TimePolicy,used:Int,grace:Int)=SessionState(
        usedMinutes=used,
        graceRemaining=grace,
        active=usageEngine.remaining(policy,used)>0 && !graceEngine.isExpired(grace)
    )
}
