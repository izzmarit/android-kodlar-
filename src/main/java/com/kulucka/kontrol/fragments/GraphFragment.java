package com.kulucka.kontrol.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.kulucka.kontrol.R;
import com.kulucka.kontrol.models.SensorData;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GraphFragment extends Fragment {
    private static final String TAG = "GraphFragment";

    // Grafik bileşeni
    private LineChart chart;

    // Kontrol elemanları
    private RadioGroup rgDataType;
    private RadioGroup rgTimeRange;
    private Button btnRefresh;
    private TextView tvNoData;

    // Veri tipleri
    private static final int DATA_TYPE_TEMPERATURE = 0;
    private static final int DATA_TYPE_HUMIDITY = 1;

    // Zaman aralıkları
    private static final int TIME_RANGE_HOUR = 0;
    private static final int TIME_RANGE_DAY = 1;
    private static final int TIME_RANGE_WEEK = 2;

    // Veri listesi
    private List<SensorData> sensorDataHistory = new ArrayList<>();
    private boolean isConnected = false;

    // Seçili filtreler
    private int selectedDataType = DATA_TYPE_TEMPERATURE;
    private int selectedTimeRange = TIME_RANGE_HOUR;

    public GraphFragment() {
        // Gerekli boş kurucu
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Görünümü inflate et
        View view = inflater.inflate(R.layout.fragment_graph, container, false);

        // Görünüm elemanlarını tanımla
        initViews(view);

        // Grafik ayarları
        setupChart();

        // Olayları ayarla
        setupListeners();

        return view;
    }

    private void initViews(View view) {
        chart = view.findViewById(R.id.chart);
        rgDataType = view.findViewById(R.id.rgDataType);
        rgTimeRange = view.findViewById(R.id.rgTimeRange);
        btnRefresh = view.findViewById(R.id.btnRefresh);
        tvNoData = view.findViewById(R.id.tvNoData);
    }

    private void setupChart() {
        // Grafik genel ayarları
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setBackgroundColor(Color.WHITE);

        // X ekseni ayarları
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new ValueFormatter() {
            private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());

            @Override
            public String getFormattedValue(float value) {
                // Unix timestamp'i saat:dakika formatına çevir
                return sdf.format(new Date((long) value));
            }
        });

        // Y ekseni ayarları
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);

        // Başlangıçta veri yok
        updateChartData();
    }

    private void setupListeners() {
        // Veri tipi seçimi
        rgDataType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbTemperature) {
                selectedDataType = DATA_TYPE_TEMPERATURE;
            } else if (checkedId == R.id.rbHumidity) {
                selectedDataType = DATA_TYPE_HUMIDITY;
            }

            updateChartData();
        });

        // Zaman aralığı seçimi
        rgTimeRange.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbHour) {
                selectedTimeRange = TIME_RANGE_HOUR;
            } else if (checkedId == R.id.rbDay) {
                selectedTimeRange = TIME_RANGE_DAY;
            } else if (checkedId == R.id.rbWeek) {
                selectedTimeRange = TIME_RANGE_WEEK;
            }

            updateChartData();
        });

        // Yenileme butonu
        btnRefresh.setOnClickListener(v -> updateChartData());
    }

    private void updateChartData() {
        if (sensorDataHistory.isEmpty()) {
            // Veri yoksa grafik yerine bilgi mesajı göster
            chart.setVisibility(View.GONE);
            tvNoData.setVisibility(View.VISIBLE);
            return;
        }

        // Veri varsa grafiği göster
        chart.setVisibility(View.VISIBLE);
        tvNoData.setVisibility(View.GONE);

        // Zaman aralığına göre filtrelenmiş veri
        List<SensorData> filteredData = filterDataByTimeRange();

        // Grafik verisi oluştur
        List<Entry> entries = new ArrayList<>();

        for (SensorData data : filteredData) {
            float value = selectedDataType == DATA_TYPE_TEMPERATURE ?
                    data.getTemperature() : data.getHumidity();

            // Unix timestamp milisaniye cinsinden
            long timestamp = System.currentTimeMillis();

            entries.add(new Entry(timestamp, value));
        }

        if (entries.isEmpty()) {
            chart.setVisibility(View.GONE);
            tvNoData.setVisibility(View.VISIBLE);
            return;
        }

        // Grafik verisi ayarla
        LineDataSet dataSet = new LineDataSet(entries,
                selectedDataType == DATA_TYPE_TEMPERATURE ? "Sıcaklık (°C)" : "Nem (%)");

        // Veri seti ayarları
        dataSet.setColor(selectedDataType == DATA_TYPE_TEMPERATURE ?
                Color.RED : Color.BLUE);
        dataSet.setCircleColor(selectedDataType == DATA_TYPE_TEMPERATURE ?
                Color.RED : Color.BLUE);
        dataSet.setDrawCircles(true);
        dataSet.setCircleRadius(4f);
        dataSet.setLineWidth(2f);
        dataSet.setValueTextSize(10f);

        // Grafiğe veriyi ekle
        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);

        // Grafiği güncelle
        chart.invalidate();
    }

    private List<SensorData> filterDataByTimeRange() {
        List<SensorData> filteredData = new ArrayList<>();
        long currentTime = System.currentTimeMillis();
        long cutoffTime;

        // Zaman aralığına göre kesme zamanı belirle
        switch (selectedTimeRange) {
            case TIME_RANGE_HOUR:
                cutoffTime = currentTime - (60 * 60 * 1000); // 1 saat
                break;
            case TIME_RANGE_DAY:
                cutoffTime = currentTime - (24 * 60 * 60 * 1000); // 1 gün
                break;
            case TIME_RANGE_WEEK:
                cutoffTime = currentTime - (7 * 24 * 60 * 60 * 1000); // 1 hafta
                break;
            default:
                cutoffTime = 0;
                break;
        }

        // Kesme zamanından sonraki verileri filtrele
        for (SensorData data : sensorDataHistory) {
            if (System.currentTimeMillis() >= cutoffTime) {
                filteredData.add(data);
            }
        }

        return filteredData;
    }

    // Son alınan veriyi geçmiş veriye ekle
    private void addToHistory(SensorData data) {
        // Veri geçmişini belirli bir boyutta tut
        if (sensorDataHistory.size() >= 1000) {
            sensorDataHistory.remove(0);
        }

        sensorDataHistory.add(data);
    }

    // MainActivity'den çağrılan güncelleme metodları
    public void updateConnectionStatus(boolean connected) {
        this.isConnected = connected;

        if (isAdded()) {
            btnRefresh.setEnabled(connected);
        }
    }

    public void updateSensorData(SensorData sensorData) {
        if (isAdded() && sensorData != null) {
            // Yeni veriyi geçmiş veriye ekle
            addToHistory(sensorData);

            // Otomatik olarak grafiği güncelle
            updateChartData();
        }
    }
}