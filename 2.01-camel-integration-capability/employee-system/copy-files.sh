podName=$(oc get pods -l component=employee-system -o custom-columns=NAME:.metadata.name --no-headers)

files="employee-backend-rules.yaml employee-backend.camel.yaml"

for file in $files ; do
  oc cp $file ${podName}:/data/$file
done