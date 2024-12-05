package com.epinephrine00.yogallflow

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Space
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.setPadding
import androidx.navigation.fragment.findNavController
import com.epinephrine00.yogallflow.databinding.FragmentSecondBinding
import com.epinephrine00.yogallflow.MainActivity.Companion.activity as mainActivity

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SecondFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    lateinit var redBar:SeekBar
    lateinit var greenBar:SeekBar
    lateinit var blueBar:SeekBar
    lateinit var redET:EditText
    lateinit var greenET:EditText
    lateinit var blueET:EditText

    lateinit var durations:ArrayList<Int>
    lateinit var ledList:ArrayList<ArrayList<ArrayList<Int>>>
    // [ [ [r, g, b], [r, g, b], [r, g, b] ...(12*rgb)... ], [ [r, g, b], ... ], ... ]

    lateinit var LEDs:ArrayList<Button>
    lateinit var colors:ArrayList<Int>
    lateinit var colorEntry:ArrayList<ArrayList<Int>>

    lateinit var duration:EditText

    lateinit var addButton: Button
    lateinit var listLinearLayout: LinearLayout

    lateinit var completeButton : Button



    var currentSelectedLED:Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSecondBinding.inflate(inflater, container, false)

        LEDs = arrayListOf(binding.led0, binding.led1, binding.led2,  binding.led3,
                           binding.led4, binding.led5, binding.led6,  binding.led7,
                           binding.led8, binding.led9, binding.led10, binding.led11)

        durations = ArrayList()
        ledList = ArrayList()

        colors = arrayListOf(Color.rgb(127, 127, 127), Color.rgb(127, 127, 127),
            Color.rgb(127, 127, 127), Color.rgb(127, 127, 127),
            Color.rgb(127, 127, 127), Color.rgb(127, 127, 127),
            Color.rgb(127, 127, 127), Color.rgb(127, 127, 127),
            Color.rgb(127, 127, 127), Color.rgb(127, 127, 127),
            Color.rgb(127, 127, 127), Color.rgb(127, 127, 127),)
        colorEntry = arrayListOf(arrayListOf(50, 50, 50),arrayListOf(50, 50, 50),
            arrayListOf(50, 50, 50),arrayListOf(50, 50, 50),
            arrayListOf(50, 50, 50),arrayListOf(50, 50, 50),
            arrayListOf(50, 50, 50),arrayListOf(50, 50, 50),
            arrayListOf(50, 50, 50),arrayListOf(50, 50, 50),
            arrayListOf(50, 50, 50),arrayListOf(50, 50, 50),)

        for(i in 0..11) {
            LEDs[i].setOnClickListener { currentSelectedLED = i; Log.d("SelectedLED", "$i"); changeColor()}
            setColor(i)
        }
        redBar   = binding.redSlider
        greenBar = binding.greenSlider
        blueBar  = binding.blueSlider

        redET    = binding.redValue
        greenET  = binding.greenValue
        blueET   = binding.blueValue

        duration  = binding.duration
        addButton = binding.addButton
        completeButton = binding.complete

        listLinearLayout = binding.horizontalLinearLayoutBelow

        redBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                redET.setText(p0!!.progress.toString())
                changeColor()
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {
                redET.setText(p0!!.progress.toString())
                changeColor()
            }
            override fun onStopTrackingTouch(p0: SeekBar?) {
                redET.setText(p0!!.progress.toString())
                changeColor()
            }
        })
        greenBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                greenET.setText(p0!!.progress.toString())
                changeColor()
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {
                greenET.setText(p0!!.progress.toString())
                changeColor()
            }
            override fun onStopTrackingTouch(p0: SeekBar?) {
                greenET.setText(p0!!.progress.toString())
                changeColor()
            }
        })
        blueBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                blueET.setText(p0!!.progress.toString())
                changeColor()
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {
                blueET.setText(p0!!.progress.toString())
                changeColor()
            }
            override fun onStopTrackingTouch(p0: SeekBar?) {
                blueET.setText(p0!!.progress.toString())
                changeColor()
            }
        })

        redET.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                val inputText = s.toString()
                if (inputText.isNotEmpty()) {
                    var value = inputText.toIntOrNull() ?: 0
                    when {
                        value > 100 -> value = 100
                        value < 0 -> value = 0
                    }
                    if (value.toString() != inputText) {
                        redET.setText(value.toString())
                        redET.setSelection(redET.text.length)
                    }
                    redBar.progress = value
                }
                changeColor()
            }
        })
        greenET.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                val inputText = s.toString()
                if (inputText.isNotEmpty()) {
                    var value = inputText.toIntOrNull() ?: 0
                    when {
                        value > 100 -> value = 100
                        value < 0 -> value = 0
                    }
                    if (value.toString() != inputText) {
                        greenET.setText(value.toString())
                        greenET.setSelection(greenET.text.length)
                    }
                    greenBar.progress = value
                }
                changeColor()
            }
        })

        blueET.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                val inputText = s.toString()
                if (inputText.isNotEmpty()) {
                    var value = inputText.toIntOrNull() ?: 0
                    when {
                        value > 100 -> value = 100
                        value < 0 -> value = 0
                    }
                    if (value.toString() != inputText) {
                        blueET.setText(value.toString())
                        blueET.setSelection(blueET.text.length)
                    }
                    blueBar.progress = value
                }
                changeColor()
            }
        })

        addButton.setOnClickListener {
            durations.add(duration.text.toString().toInt())
            ledList.add(colorEntry)
            val tmp = colorEntry
            colorEntry = arrayListOf(arrayListOf(50, 50, 50),arrayListOf(50, 50, 50),
                arrayListOf(50, 50, 50),arrayListOf(50, 50, 50),
                arrayListOf(50, 50, 50),arrayListOf(50, 50, 50),
                arrayListOf(50, 50, 50),arrayListOf(50, 50, 50),
                arrayListOf(50, 50, 50),arrayListOf(50, 50, 50),
                arrayListOf(50, 50, 50),arrayListOf(50, 50, 50),)
            for(i in 0..11)
                for(j in 0..2)
                    colorEntry[i][j] = tmp[i][j]
            Log.d("isAddingCOlorEntryMondai", "$ledList")
            renderSets()
        }

        completeButton.setOnClickListener {
            val editText = EditText(mainActivity)
            editText.hint = "LED 시퀀스의 이름을 입력해주세요"
            val builder = AlertDialog.Builder(mainActivity)
                .setTitle("LED 시퀀스 이름")
                .setView(editText)
                .setPositiveButton("확인") { dialog, _ ->
                    mainActivity.addLEDSequence(durations, ledList, editText.text.toString())
                    findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
                    dialog.dismiss()
                }
                .setNegativeButton("취소") { dialog, _ ->
                    // 취소 버튼을 눌렀을 때 동작
                    dialog.dismiss()
                }
            builder.create().show()
        }



        return binding.root

    }

    fun changeColor(){

        var red = redET.text.toString().toIntOrNull() ?: 0
        var green = greenET.text.toString().toIntOrNull() ?: 0
        var blue = blueET.text.toString().toIntOrNull() ?: 0

        colorEntry[currentSelectedLED] = arrayListOf(red, green, blue)

        red = ((red/100f)*255f).toInt()
        green = ((green/100f)*255f).toInt()
        blue = ((blue/100f)*255f).toInt()

        colors[currentSelectedLED] = Color.rgb(red, green, blue)
        setColor(currentSelectedLED)
    }

    fun setColor(index:Int){
        //Log.d("parsecolor", "${Color.parseColor("#0000ff")}")
        LEDs[index].setBackgroundColor(colors[index])
    }

    fun renderSets(){
        listLinearLayout.removeAllViews()
        for(i in 0..<durations.size){
            val linearLayouttmp = LinearLayout(mainActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
            }
            var d = durations[i]
            var colorSet = ledList[i]
            Log.d("isColorMondat", i.toString())
            Log.d("isColorMondai", "$colorSet")

            var constraintLayout = ConstraintLayout(mainActivity).apply{
                layoutParams = LayoutParams(130, 150)
                    //LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                setPadding(16)
            }
            for(j in 0..5)
                constraintLayout.addView(createRotatedLinearLayout(j*30f, colorSet[j], colorSet[6+j]))
            linearLayouttmp.addView(constraintLayout)
            var durationtv = TextView(mainActivity).apply{
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                text = d.toString()
                textSize = 16f
                gravity = Gravity.CENTER
            }
            linearLayouttmp.addView(durationtv)
            listLinearLayout.addView(linearLayouttmp)
        }
    }
    fun createRotatedLinearLayout(rotation: Float, color1:ArrayList<Int>, color2:ArrayList<Int>): LinearLayout {
        return LinearLayout(mainActivity).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            this.rotation = rotation

            // First Button
            val button1 = Button(mainActivity).apply {
                layoutParams = LinearLayout.LayoutParams(15, 17)
            }
            var r = ((color1[0]/100f)*255f).toInt()
            var g = ((color1[1]/100f)*255f).toInt()
            var b = ((color1[2]/100f)*255f).toInt()
            button1.setBackgroundColor(Color.rgb(r,g,b))

            // Space between Buttons
            val space = Space(mainActivity).apply {
                layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 50)
            }

            // Second Button
            val button2 = Button(mainActivity).apply {
                layoutParams = LinearLayout.LayoutParams(15, 17)
            }
            r = ((color2[0]/100f)*255f).toInt()
            g = ((color2[1]/100f)*255f).toInt()
            b = ((color2[2]/100f)*255f).toInt()
            button2.setBackgroundColor(Color.rgb(r,g,b))

            addView(button1)
            addView(space)
            addView(button2)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonSecond.setOnClickListener {
            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}