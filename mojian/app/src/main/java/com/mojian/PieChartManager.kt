package com.mojian

import android.graphics.Color
import com.github.mikephil.charting.charts.PieChart
import android.graphics.Typeface
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.formatter.PercentFormatter

/**
 * Author: pengmutian
 * Date: 2022/10/21 21:50
 * Description: PieChartManagger
 */
/**
 * Created by zhang on 2018/7/5.
 */
class PieChartManager(var pieChart: PieChart) {
    //初始化
    private fun initPieChart() {
        // 半透明圈
        pieChart.transparentCircleRadius = 30f
        pieChart.setTransparentCircleColor(Color.WHITE) //设置半透明圆圈的颜色
        pieChart.setTransparentCircleAlpha(125) //设置半透明圆圈的透明度

//        //饼状图中间可以添加文字
//        pieChart.setDrawCenterText(false)
//        pieChart.centerText = "民族" //设置中间文字
//        pieChart.setCenterTextColor(Color.parseColor("#a1a1a1")) //中间问题的颜色
//        pieChart.setCenterTextSizePixels(36f) //中间文字的大小px
//        pieChart.centerTextRadiusPercent = 1f
//        pieChart.setCenterTextTypeface(Typeface.DEFAULT) //中间文字的样式
//        pieChart.setCenterTextOffset(0f, 0f) //中间文字的偏移量
//        pieChart.rotationAngle = 0f // 初始旋转角度
//        pieChart.isRotationEnabled = true // 可以手动旋转
//        pieChart.setUsePercentValues(true) //显示成百分比
//        pieChart.description.isEnabled = false //取消右下角描述

        //是否显示每个部分的文字描述
        pieChart.setDrawEntryLabels(false)
        pieChart.setEntryLabelColor(Color.RED) //描述文字的颜色
        pieChart.setEntryLabelTextSize(14f) //描述文字的大小
        pieChart.setEntryLabelTypeface(Typeface.DEFAULT_BOLD) //描述文字的样式

        //图相对于上下左右的偏移
        pieChart.setExtraOffsets(20f, 8f, 75f, 8f)
        //图标的背景色
        pieChart.setBackgroundColor(Color.TRANSPARENT)
        //        设置pieChart图表转动阻力摩擦系数[0,1]
        pieChart.dragDecelerationFrictionCoef = 0.75f

        //获取图例
        val legend = pieChart.legend
        legend.orientation = Legend.LegendOrientation.VERTICAL //设置图例水平显示
        legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP //顶部
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT //右对其
        legend.xEntrySpace = 7f //x轴的间距
        legend.yEntrySpace = 10f //y轴的间距
        legend.yOffset = 10f //图例的y偏移量
        legend.xOffset = 10f //图例x的偏移量
        legend.textColor = Color.parseColor("#a1a1a1") //图例文字的颜色
        legend.textSize = 13f //图例文字的大小
    }

    /**
     * 显示实心圆
     * @param yvals
     * @param colors
     */
    fun showSolidPieChart(yvals: List<PieEntry?>?, colors: List<Int?>?) {
        //数据集合
        val dataset = PieDataSet(yvals, "")
        //填充每个区域的颜色
        dataset.colors = colors
        //是否在图上显示数值
        dataset.setDrawValues(true)
        //        文字的大小
        dataset.valueTextSize = 14f
        //        文字的颜色
        dataset.valueTextColor = Color.RED
        //        文字的样式
        dataset.valueTypeface = Typeface.DEFAULT_BOLD

//      当值位置为外边线时，表示线的前半段长度。
        dataset.valueLinePart1Length = 0.4f
        //      当值位置为外边线时，表示线的后半段长度。
        dataset.valueLinePart2Length = 0.4f
        //      当ValuePosits为OutsiDice时，指示偏移为切片大小的百分比
        dataset.valueLinePart1OffsetPercentage = 80f
        // 当值位置为外边线时，表示线的颜色。
        dataset.valueLineColor = Color.parseColor("#a1a1a1")
        //        设置Y值的位置是在圆内还是圆外
        dataset.yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        //        设置Y轴描述线和填充区域的颜色一致
        dataset.isUsingSliceColorAsValueLineColor = false
        //        设置每条之前的间隙
        dataset.sliceSpace = 0f

        //设置饼状Item被选中时变化的距离
        dataset.selectionShift = 5f
        //填充数据
        val pieData = PieData(dataset)
        //        格式化显示的数据为%百分比
        pieData.setValueFormatter(PercentFormatter())
        //        显示试图
        pieChart.data = pieData
    }

    /**
     * 显示圆环
     * @param yvals
     * @param colors
     */
    fun showRingPieChart(yvals: List<PieEntry?>?, colors: List<Int?>?) {
        //显示为圆环
        pieChart.isDrawHoleEnabled = true
        pieChart.holeRadius = 85f //设置中间洞的大小
        pieChart.centerText = "TES"
        pieChart.setDrawCenterText(true)
        pieChart.setCenterTextColor(Color.BLACK)

        //数据集合
        val dataset = PieDataSet(yvals, "")
        //填充每个区域的颜色
        dataset.colors = colors
        //是否在图上显示数值
        dataset.setDrawValues(true)
        //        文字的大小
        dataset.valueTextSize = 14f
        //        文字的颜色
        dataset.valueTextColor = Color.RED
        //        文字的样式
        dataset.valueTypeface = Typeface.DEFAULT_BOLD

//      当值位置为外边线时，表示线的前半段长度。
        dataset.valueLinePart1Length = 0.4f
        //      当值位置为外边线时，表示线的后半段长度。
        dataset.valueLinePart2Length = 0.4f
        //      当ValuePosits为OutsiDice时，指示偏移为切片大小的百分比
        dataset.valueLinePart1OffsetPercentage = 80f
        // 当值位置为外边线时，表示线的颜色。
        dataset.valueLineColor = Color.parseColor("#a1a1a1")
        //        设置Y值的位置是在圆内还是圆外
        dataset.yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        //        设置Y轴描述线和填充区域的颜色一致
        dataset.isUsingSliceColorAsValueLineColor = false
        //        设置每条之前的间隙
        dataset.sliceSpace = 0f

        //设置饼状Item被选中时变化的距离
        dataset.selectionShift = 5f
        //填充数据
        val pieData = PieData(dataset)
        //        格式化显示的数据为%百分比
        pieData.setValueFormatter(PercentFormatter())
        //        显示试图
        pieChart.data = pieData

    }

    init {
        initPieChart()
    }
}