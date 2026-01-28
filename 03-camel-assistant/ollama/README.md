# Ollama Setup

1. Deploy
```shell
oc apply -f ollama.yaml
```

2. Expose the service
```shell
oc expose svc ollama
```

3. Expose the host
```shell
export OLLAMA_HOST=$(echo "http://$(oc get route ollama -o jsonpath='{.spec.host}')")
```


4. Pull the embedded model 
```shell
ollama pull nomic-embed-text:latest
```