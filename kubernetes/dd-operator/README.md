


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

# Node agent tolerations

The `DatadogAgent` manifests (`datadog-agent.yaml` and
`prometheus-pod-annotation-scrapping/datadog-agent-otel-collector-prometheus-annotation-scrapping.yaml`)
define **no tolerations** on the node agent: the node agent DaemonSet only runs on nodes without taints.
This fits the clusters used for this demo (single node Rancher Desktop k3s, k3d) and managed clusters
(EKS, GKE, AKS) where control plane nodes are not schedulable.

Add tolerations under `spec.override.nodeAgent.tolerations` only when the cluster has tainted nodes that must be monitored:

* Rancher RKE1 control plane and etcd nodes
  (see https://docs.datadoghq.com/containers/kubernetes/distributions/#rancher ):
  ```yaml
  tolerations:
    - key: "node-role.kubernetes.io/controlplane"
      operator: "Exists"
      effect: "NoSchedule"
    - key: "node-role.kubernetes.io/etcd"
      operator: "Exists"
      effect: "NoExecute"
  ```
* kubeadm and most upstream distributions taint control plane nodes with `node-role.kubernetes.io/control-plane:NoSchedule`.
* Run on every node whatever its taints (also tolerates `NoExecute` taints added on node problems, use deliberately):
  ```yaml
  tolerations:
    - operator: "Exists"
  ```
