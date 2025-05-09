package com.example.kuluckakontrolu.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.kuluckakontrolu.R
import com.example.kuluckakontrolu.databinding.FragmentGraphBinding
import com.example.kuluckakontrolu.model.ChartType
import com.example.kuluckakontrolu.model.IncubatorData
import com.example.kuluckakontrolu.model.TimeRange
import com.example.kuluckakontrolu.viewmodel.IncubatorViewModel
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GraphFragment : Fragment() {
    private var _binding: FragmentGraphBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: IncubatorViewModel

    private var chartType = ChartType.TEMPERATURE
    private var timeRange = TimeRange.LAST_DAY

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGraphBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity()).get(IncubatorViewModel::class.java)

        setupChartTypeSpinner()
        setupTimeRangeSpinner()
        setupCharts()

        // Verileri yükle
        viewModel.loadHistoricalData(timeRange)

        // Veri değişikliklerini gözlemle
        lifecycleScope.launch {
            viewModel.historicalData.collect { data ->
                updateChart(data)
            }
        }

        lifecycleScope.launch {
            viewModel.selectedTimeRange.collect { newRange ->
                timeRange = newRange
            }
        }
    }

    private fun setupChartTypeSpinner() {
        val chartTypes = arrayOf("Sıcaklık", "Nem", "Kombine", "Isıtıcı Aktivitesi")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, chartTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spinnerChartType.adapter = adapter
        binding.spinnerChartType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                chartType = when(position) {
                    0 -> ChartType.TEMPERATURE
                    1 -> ChartType.HUMIDITY
                    2 -> ChartType.COMBINED
                    3 -> ChartType.HEATER_ACTIVITY
                    else -> ChartType.TEMPERATURE
                }

                updateChartVisibility()

                // Veriyi tekrar çiz
                viewLifecycleOwner.lifecycleScope.launch {
                    updateChart(viewModel.historicalData.value)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) { }
        }
    }

    private fun setupTimeRangeSpinner() {
        val timeRanges = arrayOf("Son 1 Saat", "Son 24 Saat", "Son 7 Gün", "Tüm Döngü")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, timeRanges)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spinnerTimeRange.adapter = adapter
        binding.spinnerTimeRange.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val newTimeRange = when(position) {
                    0 -> TimeRange.LAST_HOUR
                    1 -> TimeRange.LAST_DAY
                    2 -> TimeRange.LAST_WEEK
                    3 -> TimeRange.FULL_CYCLE
                    else -> TimeRange.LAST_DAY
                }

                if (newTimeRange != timeRange) {
                    timeRange = newTimeRange
                    viewModel.loadHistoricalData(timeRange)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) { }
        }

        // Varsayılan olarak "Son 24 Saat" seçili
        binding.spinnerTimeRange.setSelection(1)
    }

    private fun setupCharts() {
        // Sıcaklık grafiği
        binding.chartTemperature.apply {
            description.isEnabled = false
            setDrawGridBackground(false)

            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.valueFormatter = DateAxisValueFormatter()
            xAxis.labelRotationAngle = 45f
            xAxis.granularity = 1f

            axisRight.isEnabled = false

            axisLeft.textColor = Color.RED
            axisLeft.setDrawGridLines(true)

            legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER

            setTouchEnabled(true)
            isDragEnabled = true
            isScaleXEnabled = true
            isScaleYEnabled = false

            animateX(1000)
        }

        // Nem grafiği
        binding.chartHumidity.apply {
            description.isEnabled = false
            setDrawGridBackground(false)

            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.valueFormatter = DateAxisValueFormatter()
            xAxis.labelRotationAngle = 45f
            xAxis.granularity = 1f

            axisRight.isEnabled = false

            axisLeft.textColor = Color.BLUE
            axisLeft.setDrawGridLines(true)

            legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER

            setTouchEnabled(true)
            isDragEnabled = true
            isScaleXEnabled = true
            isScaleYEnabled = false

            animateX(1000)
        }

        // Kombine grafik
        binding.chartCombined.apply {
            description.isEnabled = false
            setDrawGridBackground(false)

            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.valueFormatter = DateAxisValueFormatter()
            xAxis.labelRotationAngle = 45f
            xAxis.granularity = 1f

            axisRight.setDrawGridLines(false)
            axisRight.textColor = Color.BLUE

            axisLeft.setDrawGridLines(true)
            axisLeft.textColor = Color.RED

            legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER

            setTouchEnabled(true)
            isDragEnabled = true
            isScaleXEnabled = true
            isScaleYEnabled = false

            animateX(1000)
        }

        // Aktivite grafiği
        binding.chartActivity.apply {
            description.isEnabled = false
            setDrawGridBackground(false)

            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.valueFormatter = DateAxisValueFormatter()
            xAxis.labelRotationAngle = 45f
            xAxis.granularity = 1f

            axisRight.isEnabled = false

            axisLeft.axisMinimum = -0.1f
            axisLeft.axisMaximum = 1.1f
            axisLeft.setDrawGridLines(true)

            legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER

            setTouchEnabled(true)
            isDragEnabled = true
            isScaleXEnabled = true
            isScaleYEnabled = false

            animateX(1000)
        }

        updateChartVisibility()
    }

    private fun updateChartVisibility() {
        binding.chartTemperature.visibility = if (chartType == ChartType.TEMPERATURE) View.VISIBLE else View.GONE
        binding.chartHumidity.visibility = if (chartType == ChartType.HUMIDITY) View.VISIBLE else View.GONE
        binding.chartCombined.visibility = if (chartType == ChartType.COMBINED) View.VISIBLE else View.GONE
        binding.chartActivity.visibility = if (chartType == ChartType.HEATER_ACTIVITY) View.VISIBLE else View.GONE
    }

    private fun updateChart(data: List<IncubatorData>) {
        if (data.isEmpty()) {
            binding.textNoData.visibility = View.VISIBLE
            return
        } else {
            binding.textNoData.visibility = View.GONE
        }

        when (chartType) {
            ChartType.TEMPERATURE -> updateTemperatureChart(data)
            ChartType.HUMIDITY -> updateHumidityChart(data)
            ChartType.COMBINED -> updateCombinedChart(data)
            ChartType.HEATER_ACTIVITY -> updateActivityChart(data)
        }
    }

    private fun updateTemperatureChart(data: List<IncubatorData>) {
        if (data.isEmpty()) {
            binding.textNoData.visibility = View.VISIBLE
            binding.chartTemperature.visibility = View.GONE
            return
        }

        binding.textNoData.visibility = View.GONE
        binding.chartTemperature.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
            try {
                // Veri noktalarını oluştur - null kontrolü ile
                val entries = data.mapNotNull { incubatorData ->
                    if (incubatorData.state.currentTemp > 0) {
                        Entry(incubatorData.timestamp.toFloat(), incubatorData.state.currentTemp.toFloat())
                    } else null
                }

                val targetEntries = data.mapNotNull { incubatorData ->
                    if (incubatorData.settings.targetTemp > 0) {
                        Entry(incubatorData.timestamp.toFloat(), incubatorData.settings.targetTemp.toFloat())
                    } else null
                }

                // Sensör 1 ve Sensör 2 verileri - ESP32 formatıyla uyumlu null kontrolü
                val sensor1Entries = data.mapNotNull { incubatorData ->
                    if (incubatorData.state.temp1 > 0) {
                        Entry(incubatorData.timestamp.toFloat(), incubatorData.state.temp1.toFloat())
                    } else null
                }

                val sensor2Entries = data.mapNotNull { incubatorData ->
                    if (incubatorData.state.temp2 > 0) {
                        Entry(incubatorData.timestamp.toFloat(), incubatorData.state.temp2.toFloat())
                    } else null
                }

                // Grafiği oluşturma ve gösterme kodu...
            } catch (e: Exception) {
                Log.e("GraphFragment", "Grafik güncellenirken hata: ${e.message}")
                withContext(Dispatchers.Main) {
                    binding.textNoData.visibility = View.VISIBLE
                    binding.textNoData.text = getString(R.string.generic_error, e.localizedMessage)
                    binding.chartTemperature.visibility = View.GONE
                }
            }
        }
    }

    private fun updateHumidityChart(data: List<IncubatorData>) {
        if (data.isEmpty()) {
            binding.textNoData.visibility = View.VISIBLE
            binding.chartHumidity.visibility = View.GONE
            return
        }

        binding.textNoData.visibility = View.GONE
        binding.chartHumidity.visibility = View.VISIBLE

        // Hesaplamaları arka planda yap
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
            try {
                // Ana nem verileri
                val entries = data.map {
                    Entry(it.timestamp.toFloat(), it.state.currentHumidity.toFloat())
                }

                val targetEntries = data.map {
                    Entry(it.timestamp.toFloat(), it.settings.targetHumidity.toFloat())
                }

                // Sensör 1 ve Sensör 2 verilerini ekleyelim
                val sensor1Entries = data.map {
                    Entry(it.timestamp.toFloat(), it.state.humidity1.toFloat())
                }

                val sensor2Entries = data.map {
                    Entry(it.timestamp.toFloat(), it.state.humidity2.toFloat())
                }

                // Veri setlerini oluştur
                val dataSet = LineDataSet(entries, "Nem").apply {
                    color = Color.BLUE
                    lineWidth = 2f
                    setDrawCircles(false)
                    setDrawValues(false)
                    axisDependency = YAxis.AxisDependency.LEFT
                    mode = LineDataSet.Mode.CUBIC_BEZIER
                }

                val targetDataSet = LineDataSet(targetEntries, "Hedef").apply {
                    color = Color.DKGRAY
                    lineWidth = 1f
                    enableDashedLine(10f, 5f, 0f)
                    setDrawCircles(false)
                    setDrawValues(false)
                    axisDependency = YAxis.AxisDependency.LEFT
                }

                // Sensör veri setlerini oluştur
                val sensor1DataSet = LineDataSet(sensor1Entries, "Sensör 1").apply {
                    color = Color.MAGENTA
                    lineWidth = 1.5f
                    setDrawCircles(false)
                    setDrawValues(false)
                    axisDependency = YAxis.AxisDependency.LEFT
                    enableDashedLine(5f, 5f, 0f)
                }

                val sensor2DataSet = LineDataSet(sensor2Entries, "Sensör 2").apply {
                    color = Color.CYAN
                    lineWidth = 1.5f
                    setDrawCircles(false)
                    setDrawValues(false)
                    axisDependency = YAxis.AxisDependency.LEFT
                    enableDashedLine(5f, 5f, 0f)
                }

                val lineData = LineData(dataSet, targetDataSet, sensor1DataSet, sensor2DataSet)

                // UI güncellemelerini ana thread'de yap
                withContext(Dispatchers.Main) {
                    binding.chartHumidity.data = lineData
                    binding.chartHumidity.invalidate()
                }
            } catch (e: Exception) {
                Log.e("GraphFragment", "Nem grafiği güncellenirken hata: ${e.message}")
                withContext(Dispatchers.Main) {
                    binding.textNoData.visibility = View.VISIBLE
                    binding.textNoData.text = getString(R.string.generic_error, e.localizedMessage)
                    binding.chartHumidity.visibility = View.GONE
                }
            }
        }
    }

    private fun updateCombinedChart(data: List<IncubatorData>) {
        val tempEntries = data.map {
            Entry(it.timestamp.toFloat(), it.state.currentTemp.toFloat())
        }

        val humEntries = data.map {
            Entry(it.timestamp.toFloat(), it.state.currentHumidity.toFloat())
        }

        val tempDataSet = LineDataSet(tempEntries, "Sıcaklık").apply {
            color = Color.RED
            lineWidth = 2f
            setDrawCircles(false)
            setDrawValues(false)
            axisDependency = YAxis.AxisDependency.LEFT
        }

        val humDataSet = LineDataSet(humEntries, "Nem").apply {
            color = Color.BLUE
            lineWidth = 2f
            setDrawCircles(false)
            setDrawValues(false)
            axisDependency = YAxis.AxisDependency.RIGHT
        }

        val lineData = LineData(tempDataSet, humDataSet)
        binding.chartCombined.data = lineData
        binding.chartCombined.invalidate()
    }

    private fun updateActivityChart(data: List<IncubatorData>) {
        val heaterEntries = data.map {
            Entry(it.timestamp.toFloat(), if (it.state.heaterStatus) 1f else 0f)
        }

        val humidifierEntries = data.map {
            Entry(it.timestamp.toFloat(), if (it.state.humidifierStatus) 1f else 0f)
        }

        val heaterDataSet = LineDataSet(heaterEntries, "Isıtıcı").apply {
            color = Color.RED
            lineWidth = 2f
            setDrawCircles(false)
            setDrawValues(false)
            axisDependency = YAxis.AxisDependency.LEFT
        }

        val humidifierDataSet = LineDataSet(humidifierEntries, "Nemlendirici").apply {
            color = Color.BLUE
            lineWidth = 2f
            setDrawCircles(false)
            setDrawValues(false)
            axisDependency = YAxis.AxisDependency.LEFT
        }

        val lineData = LineData(heaterDataSet, humidifierDataSet)
        binding.chartActivity.data = lineData
        binding.chartActivity.invalidate()
    }

    inner class DateAxisValueFormatter : ValueFormatter() {
        private val dateFormat = SimpleDateFormat("HH:mm\ndd/MM", Locale.getDefault())

        override fun getFormattedValue(value: Float): String {
            // value, milisaniye cinsinden zaman damgasıdır
            return dateFormat.format(Date(value.toLong()))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}