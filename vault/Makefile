PROJECT_NAME = vault
DEPLOYMENTS =  $(wildcard k8s/*.yaml)

.PHONY:
deploy:
	$(foreach f, $(DEPLOYMENTS), kubectl apply -f $f;)

.PHONY:
delete:
	-$(foreach f, $(DEPLOYMENTS), kubectl delete -f $f;)