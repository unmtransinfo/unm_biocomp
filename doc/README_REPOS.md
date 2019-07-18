# Dependencies with `unm_biocomp_*` other repositories.

This repository depends on other <https://github.com/unmtransinfo/>
repositories, public and private.

This should work to clone and install from scratch.


```
for repo in \
	unm_biocomp_util \
	unm_biocomp_depict \
	unm_biocomp_text \
	unm_biocomp_hscaf \
	unm_biocomp_sasa \
	unm_biocomp_cdk \
	unm_biocomp_biobyte \
	unm_biocomp_vcclab \
	unm_biocomp_descriptors \
	unm_biocomp ; do
	git clone "https://github.com/unmtransinfo/${repo}"
	(cd ${repo} ; mvn clean install)
done 
```
