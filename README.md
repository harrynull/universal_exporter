# Universal Exporter

Universal Prometheus Exporter for GT:NH (Minecraft 1.7.10)

You can export:

* Gauge
    * Any tileEntity fields (accessed with Java reflection)
        * e.g. single block item or fluid slots
        * storage drawer item amount
* Counter
    * number of cycle processed
    * amount of items outputed
    * amount of fluids outputed
* AE
    * amount of items in the system
    * amount of fluids in the system
    * status of crafting CPUs

