# Cloud Foundry application instance scaler
A Spring Cloud Stream sink module for invoking REST calls for a specific Cloud Foundry application.

The idea is to have a Spring Cloud Streams source(s) gather the metrics, some number of processors apply business logic and emit a simple "UP", "DOWN", or "HOLD" payload for this module to drive the Cloud Foundry API to scale up or down an application of interest.

Based entirely off the great work of [Christopher Decelles'](https://github.com/decelc-pivotal/custom-app-autoscaler/) Ops Manager Tile for doing a very similar thing.
