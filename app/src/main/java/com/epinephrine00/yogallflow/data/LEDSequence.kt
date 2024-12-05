package com.epinephrine00.yogallflow.data

data class LEDSequence(
    val duration: ArrayList<Int>,
    val ledList: ArrayList< ArrayList< ArrayList< Int > > >,
    val name:String
)
