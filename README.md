# Universal Exporter

Universal [Prometheus](https://prometheus.io/) Exporter for GT:NH (Minecraft 1.7.10)

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

## Set-up / Usage

1. Install the mod, a Prometheus endpoint will be listening at 0.0.0.0:9400

* You can change the config at `config/universal_exporter.cfg`, including host, port, and update interval.
* Start the game and verify that you can access `http://server_ip:9400/metrics` in your browser. You might need to
  adjust firewall settings.

2. Set up grafana.

* If you're hosting your Minecraft server in a dedicated VPS, you might consider just hosting your own with docker.
* The SaaS version of grafana needs an HTTPS endpoint. So you might need to reverse-proxy it.

3. Gave yourself a data wand: `/give @p universal_exporter:data_wand`.
4. Shift-right click any block of interest, and a GUI will pop out.

* Name: This will be the name of the metric exported.
* Labels: In format of `key1=value1,key2=value2`

5. Right click air and another GUI will show up with all metrics currently being tracked.

## Examples

1. Number of items in a storage drawer: Gauge with accessor `inventory.group.drawers.0.count`
2. Temperature of a coal boiler: Gauge with accessor `mTemperature`
3. The number of processing done by a EBF: Counter with trigger `machineProcessed`. Note: its value is affected by batch
   mode and parallel. To get the number of items produced, use `machineOutputItems`
4. Energy stored in a LSC: Gauge with accessor `stored`
5. Energy capacity in a LSC: Gauge with accessor `capacity`
6. Items/fluids/crafting cpu status of an AE system: RClick on any AE controller and select AE mode

## Disclaimer

This mod isn't throughly tested. Counters might not work reliably for all types of multiblocks. Use extra caution if
enabling in a server with members whom you don't trust. Performance impact isn't throughly studied. Accessor might break
when the corresponing mods upgrade as it relies on implementation details of mod.

## License

MIT

