# Applications 

Mostly command-line Java applications.

## Command-line

### From `unm_biocomp_util`

```
mvn exec:java -Dexec.mainClass="edu.unm.health.biocomp.util.db.db_utils" -Dexec.args="..."
mvn exec:java -Dexec.mainClass="edu.unm.health.biocomp.util.db.mysql_utils" -Dexec.args="..."
mvn exec:java -Dexec.mainClass="edu.unm.health.biocomp.util.db.pg_utils" -Dexec.args="..."
mvn exec:java -Dexec.mainClass="edu.unm.health.biocomp.util.db.derby_utils" -Dexec.args="..."
mvn exec:java -Dexec.mainClass="edu.unm.health.biocomp.util.db.jtds_utils" -Dexec.args="..."
```

### From `unm_biocomp_depict`

```
mvn exec:java -Dexec.mainClass="edu.unm.health.biocomp.depict.mol2img_utils" -Dexec.args="..."
mvn exec:java -Dexec.mainClass="edu.unm.health.biocomp.depict.mol2img_app" -Dexec.args="..."
```

### From `unm_biocomp_fp`

```
mvn exec:java -Dexec.mainClass="edu.unm.health.biocomp.fp.fp_utils" -Dexec.args="..."
mvn exec:java -Dexec.mainClass="edu.unm.health.biocomp.fp.ecfp_utils" -Dexec.args="..."
```

### From `unm_biocomp_molalign`

```
mvn exec:java -Dexec.mainClass="edu.unm.health.biocomp.molalign.molalign_utils" -Dexec.args="..."
```

### From `unm_biocomp_qed`

```
mvn exec:java -Dexec.mainClass="edu.unm.health.biocomp.qed.QED" -Dexec.args="..."
mvn exec:java -Dexec.mainClass="edu.unm.health.biocomp.qed.qed_app" -Dexec.args="..."
```

### From `unm_biocomp_react`

```
mvn exec:java -Dexec.mainClass="edu.unm.health.biocomp.react.react_utils" -Dexec.args="..."
mvn exec:java -Dexec.mainClass="edu.unm.health.biocomp.react.standardize_utils" -Dexec.args="..."
```

### From `unm_biocomp_ro5`

```
mvn exec:java -Dexec.mainClass="edu.unm.health.biocomp.ro5.ro5_utils" -Dexec.args="..."
```

### From `unm_biocomp_sim2d`

```
mvn exec:java -Dexec.mainClass="edu.unm.health.biocomp.sim2d.sim2d" -Dexec.args="..."
```

### From `unm_biocomp_smarts`

```
mvn exec:java -Dexec.mainClass="edu.unm.health.biocomp.smarts.smarts_utils" -Dexec.args="..."
```

## Graphical

### From `unm_biocomp_molcloud`

```
mvn exec:java -Dexec.mainClass="edu.unm.health.biocomp.molcloud.MCloud" -Dexec.args="-i Infarmatik.smi -gui -v"
```
