# Dependencies with `unm_biocomp_*` other repositories.

This repository depends on other <https://github.com/unmtransinfo/>
repositories, public and private.

This should work to clone and install from scratch.


```
for repo in \
	unm_biocomp_util \
	unm_biocomp_text \
	unm_biocomp_smarts \
	unm_biocomp_depict \
	unm_biocomp_hscaf \
	unm_biocomp_sasa \
	unm_biocomp_grouping \
	unm_biocomp_cdk \
	unm_biocomp_biobyte \
	unm_biocomp_vcclab \
	unm_biocomp_descriptors \
	unm_biocomp ; do
	git clone "https://github.com/unmtransinfo/${repo}"
	(cd ${repo} ; mvn clean install)
done 
```

Or:

```
git clone git@github.com:unmtransinfo/unm_biocomp_util.git ; cd unm_biocomp_util ; mvn clean install
git clone git@github.com:unmtransinfo/unm_biocomp_text.git ; cd unm_biocomp_text ; mvn clean install
git clone git@github.com:unmtransinfo/unm_biocomp_smarts.git ; cd unm_biocomp_smarts ; mvn clean install
git clone git@github.com:unmtransinfo/unm_biocomp_depict.git ; cd unm_biocomp_depict ; mvn clean install
git clone git@github.com:unmtransinfo/unm_biocomp_hscaf.git ; cd unm_biocomp_hscaf ; mvn clean install
git clone git@github.com:unmtransinfo/unm_biocomp_sasa.git ; cd unm_biocomp_sasa ; mvn clean install
git clone git@github.com:unmtransinfo/unm_biocomp_grouping.git ; cd unm_biocomp_grouping ; mvn clean install
git clone git@github.com:unmtransinfo/unm_biocomp_cdk.git ; cd unm_biocomp_cdk ; mvn clean install
git clone git@github.com:unmtransinfo/unm_biocomp_biobyte.git ; cd unm_biocomp_biobyte ; mvn clean install
git clone git@github.com:unmtransinfo/unm_biocomp_vcclab.git ; cd unm_biocomp_vcclab ; mvn clean install
git clone git@github.com:unmtransinfo/unm_biocomp_descriptors.git ; cd unm_biocomp_descriptors ; mvn clean install
```
