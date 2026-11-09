package nl.eddict.simplewol

import es.antonborri.home_widget.HomeWidgetGlanceWidgetReceiver

class DeviceListWidgetReceiver : HomeWidgetGlanceWidgetReceiver<DeviceListWidget>() {
    override val glanceAppWidget = DeviceListWidget()
}
