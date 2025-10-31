# Running the employee backend demo

Requirements: OpenShift instance with the Wanaku operator installed.

1. Build the employee backend and push the containers 
2. Deploy the backend to openshift using the file `employee-system/openshift-deploym`oc apply -f  employee-system/openshift-deployment.yaml`
3. Apply the changes (`oc apply -f  employee-system/wanaku-demo-employee-system.yaml`)
4. Copy the files to the CIC pod: `cd employee-system && ./copy-files.sh`
5. Use your favorite framework to create an agent or use the MCP Inspector to query the tools added to Wanaku