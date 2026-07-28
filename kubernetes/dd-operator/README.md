


# Making the DDOT Collector bundled more vendor neutral

Support the coming "OSS Pipeline" config https://github.com/DataDog/opentelemetry-examples/blob/experimental-oss-config/configurations/opentelemetry-collector/daemonset.yaml

 * Collector Pod environment variables:
   * `K8S_NODE_NAME`
   * `MY_POD_IP`
 *  FS mounts:
   * `/hostfs`
 * Permissions 
   * Requires setting up ServiceAccount, (Cluster)Role, and (Cluster)RoleBinding resources 
     See https://github.com/open-telemetry/opentelemetry-collector-contrib/tree/main/processor/k8sattributesprocessor#role-based-access-control
