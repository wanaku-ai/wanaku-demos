
Build and push the container:

```shell
mvn -B clean package -Dquarkus.container-image.build=true \
    -Dquarkus.container-image.push=true \
    -Dquarkus.container-image.build=true \
    -Dquarkus.container-image.group=<repo>
```
