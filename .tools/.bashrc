## Kubernetes ##
alias k="kubectl"
alias kcluster="k config use-context $1"

# Automatisk reconnect, bruk: kpf nom-ui 8081:8080
kpf() {
  local ns="${3:-nom}" # Default
  echo $ns
  while true; do
          sleep 2
          date
          kubectl port-forward deployment/$1 $2 --namespace $ns;
  done
}
