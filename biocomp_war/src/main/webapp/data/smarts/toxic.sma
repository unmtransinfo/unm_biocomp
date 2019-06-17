#############################################################################
### toxic.sma
### authors: Oleg Ursu, Jeremy Yang
### 3 Apr 2008
#############################################################################
### refs:
###   1. "Casarett & Doull's Toxicology", 6th ed., C. D. Klaassen, ed.,
###      McGraw-Hill, 2001.
###   2. "Derivation and Validataion of Toxicophores for Mutagenicity
###      Prediction", J. Kazius, R. McGuire, R. Bursi, J. Med. Chem,
###      2005, 48, 312-320.
#############################################################################
[OX1]=[NX2][!#8;a] aromatic nitroso compound
[NX3]([OH1])([!#8])[!#8;a] aromatic hydroxylamine
CO[NX2]=O alkyl nitrate
N[NX2]=O NC(=O)N[NX2]=O nitrosamine
O1[c,C]-[c,C]1 O1[#6x3]-[c,C]1 epoxide
C1NC1 aziridine
N=[N+]=[N-] azide
C=[N+]=[N-] diazo
N=N-N triazene
#
define $N_aromazo_not [NX2][$(acS(=O)=O),$(aacS(=O)=O),$(aaacS(=O)=O),$(aaaacS(=O)=O)]
[$([NX2](c)!@;=[NX2]c);!$([$N_aromazo_not]=[$N_aromazo_not])] aromatic azo
#
cN!@;=[NX3](O)c aromatic azoxy
#
[$([OH,NH2][N,O]);!$(O=N(O)[O-])] unsubstituted hetero-atom bonded heteroatom
#
[OH1][NX2]=C oxime
#
[#6&!R2]OO[#6&!R2] 1,2-disubstituted peroxide
#
[$([OH1][NX3]C);!$([OH]Na),$([OH][NX2]=*);!$([OH1][NX3]C=[O,N])] aliphatic hydroxylamine
#
[nH2]Na aromatic hydrazine
[NH2]NC aliphatic hydrazine
[OH][NX2]=[NX2] diazohydroxyl
[Cl,Br,I]C=O aliphatic halide
[Cl,Br,I]C=O carboxylic acid halide
[N,S](!@[CX4]!@[CH2][Cl,Br,I])!@[CX4]!@[CH2][Cl,Br,I] nitrogen or sulphur mustard
SC[Cl] alpha-chlorothioalkane
[Cl,Br,I]!@[CX4]!@[CX4]O!@C beta-halo etoxy group
[Cl]C([X1])=C[X1] chloroalkene
[Cl,Br,I][CH][CH3] [Cl,Br,I][C]([Cl,Br,I,F])[CH3] 1-chloroethyl
[Cl,Br,I]C(([F,Cl,Br,I])[X1])C=C polyhaloalkene
[Cl,Br,I]C(([F,Cl,Br,I])[X1])C(=O)[c,C] polyhalocarbonyl
[cH]1[cH]ccc2c1c3c(cc2)cc[cH][cH]3 bay-region in polycyclic aromatic hydrocabons
c1ccc2cc3c4ccccc4c5ccccc5c3cc2c1 dibenz(a,c)anthracene
c1ccc2c(c1)ccc3cc4c(ccc5ccccc45)cc23 dibenz(a,h)anthracene
Cc1ccc2cc3c(ccc4ccccc34)c5CCc1c25 3-methylcholanthrene
c1ccc2c(c1)cc3ccc4cccc5ccc2c3c45 benzo(a)pyrene
Cc1c2ccccc2c(C)c3c1ccc4ccccc34 7,12-dimethylbenz(a)anthracene
c1ccc2c(c1)ccc3c4ccccc4ccc23 chrysene
c1cc2cccc3c4cccc5cccc(c(c1)c23)c45 perylene
c1ccc2c(c1)c3cccc4ccc5cccc2c5c34 benzo(e)pyrene
c1ccc2c3c(ccc2c1)[nH]c4ccc5ccccc5c34 7H-Dibenzo(c,q)carbazole
c1ccc2c(c1)cc3ccc4cccc5ccc2c3c45 Bay-region,K-region in polycyclic aromatic hydrocabons
C1C2C=CC=CC2Cc3c1ccc4ccccc34 Bay-region,K-region in polycyclic aromatic hydrocabons
[cH]1cccc2c1[cH][cH]c3c2ccc[cH]3 K-region in polycyclic aromatic hydrocarbons
[$([C,c]OS((=O)=O)O!@[c,C]),$([c,C]S((=O)=O)O!@[c,C])] sulphonate-bonded carbon
O=N(~O)N aliphatic N-nitro
[$(O=[CH]C=C),$(O=[CH]C=O)] [$(O=[CH]C[N,O,S]=C),$(O=[CH]C=C[N,O,S]),$(O=[CH]C=Ca)] alpha-beta-unsaturated aldehyde
[Nv4]#N diazonium
[O,S]=C1CCO1 beta-propiolactone
[CH]=[CH]O alpha,beta unsaturated alkoxy group
[NH1;!R](C)[NH;!R]a 1-aryl-2monoalkyl hydrazine
#
[$([CH3][NH;!R]a);!$([CH3][NH]a[$(a[$(C((F)F)F),$(S=O),$(C(=O)=O)]),$(aa[$(C((F)F)F),$(S=O),$(C(=O)O)]),$(aaa[$(C((F)F)F),$(S=O),$(C(=O)=O)])])] aromatic methylamine
#
aN([$([OH]),$(O*=O)])[$([#1]),$(C(=O)[CH3]),$([CH3]),$([OH]),$(O*=O)] ester derivative of aromatic hydroxylamine
c13~c~c~c~c2~c1~c(~c~c~c3)~c~c~c2 polycyclic aromatic
c1~c~c~c2~c1~c~c3~c(~c2)~c~c~c3 polycyclic aromatic
c1~c~c~c2~c1~c~c~c3~c2~c~c~c3 polycyclic aromatic
c1~c~c~c~c2~c1~c3~c(~c2)~c~c~c~c3 polycyclic aromatic
c1~c~c~c~c2~c1~c~c3~c(~c2)~c~c~c3 polycyclic aromatic
c1~c~c~c~c2~c1~c~c3~c(~c2)~c~c~c~c3 polycyclic aromatic
c1~c~c~c~c2~c1~c~c~c3~c2~c~c~c3 polycyclic aromatic
c1~c~c~c~c2~c1~c~c~c3~c2~c~c~c~c3 polycyclic aromatic
c13~c~c~c~c2~c1~c(~c~c~c3)~c~c2 polycyclic aromatic
[#7X3]([F,Cl,Br,I])[c,C]=O N-Haloimide
[SX4](=O)(=O)OO[SX4](=O)=O persulphate
S(=O)(=O)([NX3][F,Cl,Br,I])c aryl N-chlorosulphonamide
[$([CH2]([OH])c1cccc2c1cccc2),$([CH3]c1cccc2c1cccc2),$(C(=O)O[CH2]c1cccc2c1cccc2)] polycyclic aromatic methyl compound arylhydroxymethyl compound or ester derivative
C[N;!R]=C=O isocyanate
[$([NX3H1;!R](C)[NH2;!R]),$([NX3H0;!R](C)(C)[NH2;!R])] mono- or di-alkylhydrazine
[NX3;!R]C(=S)[NX3;!R] thiourea
[C;!R](=O)(C=C)C=C substituted vinyl ketone
c1(=O)oc2c(cc1)cc3ccoc3c2 psoralen
C(=O)N=[N+]=[N-] acid azide
S(=O)(=O)([#6])[F,Cl,Br,I] sulphonyl halide
S(=O)(=O)([#6])N=[N+]=[N-] sulphonyl azide
C(=O)OC=O acid anhydride or analogue
O=C(Oc1ccccc1)Oc2ccccc2 phenyl carbonate
CN=C=S isothiocyanate
[C;!R](=S)[SX2;!R] thioester
[$(S(=O)(=O)(OC)C),$(S(=O)(=O)(O)C)] alkyl sulphate or sulphonate
O=C1C=CC(=O)C=C1 quinone
C[CH1]=O aldehyde
[c,C][C;!R](=O)C[C;!R](=O)[c,C] 1,3-diketone
#
[$([C;!R](OC)(OC)[c,C]);!$([C;!R](OC=O)(OC=O)[c,C])] aldehyde precursor
#
c1(=O)nscc1 isothiazolinone
[NX3;!R][CX4;!R][CX4;!R][NX3;!R] diamine
c1([F,Cl,Br,I,O])nc([F,Cl,Br,I])nc([F,Cl,Br,I,O])n1 activated N-heterocycle
c1([F,Cl,Br,I,OH1;!R])ncccc1 activated pyridine quinoline or isoquinoline
[OH1]c1cccc([OH1])c1[OH1] gallate or precursor
NC(=S)SC(N)=S thiuram mono- or di-sulphide
[$(C=CC=NC),$(C=CCC=NC)] imine or alpha,beta-unsaturated imine
[$(CSS(C)(=O)=O),$(CS(S)(=O)=O)] thiosulphate or thiosulphonate
[$([c,C]S(=O)(=O)C=C),$([c,C]S(=O)(=O)CC=C)] alpha,beta-unsaturated sulphone
[$(NN(=O)=O),$(NN=O)] N-Nitro or N-nitroso compound
#
[$(N#CN),$(N#C[F,Cl,Br,I,O]);!$(N#CN-,=C(-,=N)-,=N)] cyanate cyanamide or cyanogen halide
#
c1ccc(cc1N(=O)=O)N(=O)=O di- or tri-nitro aromatic compound
[O-][n+]1ccccc1 aromatic N-oxide
C(=[NX2])=[NX2] carbodiimide
NC(=O)C[F,Cl,Br,I] haloacetanilide or analogue
O=C1C=CC1=O squarate
O=COCc1cccc2ccccc12 ester of polycyclic arylhydroxymethyl compound
CCP(=O)(OC)SC alkyl ester of phosphoric or phosphonic acid
N[C;!R](=O)[C;!R]=[C;!R] acrylamide or glycidamide
O=N(=O)c1nccn1 nitroimidazole
C(=S)=S carbon disulphide or precursor
O=CC=O 1,2-dicarbonyl compound or precursor
CC(C)=CC1C(C(=O)OCc2coc(c2)-c3ccccc3)C1(C)C pyrethroid
NC(=O)OCC#C aryl acetylenic carbamate
cN=Nc benzidine-based bisazo compound
Nc1c2ccccc2nc3ccccc13 acridine
SC#N thiocyanate
O=c1ccc2ccccc2o1 coumarin
CON(OC)C(C)=O N-Acyloxy-N-alkoxyamide
[$(C=C[CH1]=O),$(CO[CH1](OC)C=C),$(CO[CH1](CC=C)OC),$(C=CC[CH1]=O)] alpha,beta-Unsaturated aldehyde or precursor
[$(C[C;!R](=O)C=C),$(C[C;!R](=O)CC=C)] alpha,beta-unsaturated ketone or precursor
#
[$(C=CN(=O)=O),$(C=CCN(=O)=O);!$(C(-,=N)(-,=N)=CN(=O)=O)] alpha,beta-Unsaturated nitro compound or precursor
#
[$(C(=S)[N;!R]),$([N;!R]C(=S)[N;!R])] thioamide or thiourea
C=[C;!R][F,Cl,Br,I] halogenated alkene
B([c,C])([c,C])[c,C] boron alkyl
[$([BH3]),$([BH2]),$([BH1])] boron hydride
[$([BX3]([F,Cl,Br,I])[F,Cl,Br,I]),$([BX3]([F,Cl,Br,I])([F,Cl,Br,I])[F,Cl,Br,I]),$([BX3][F,Cl,Br,I])] boron halide
[$([SiX4]([F,Cl,Br,I])([F,Cl,Br,I])[F,Cl,Br,I]),$([SiX4]([F,Cl,Br,I])([F,Cl,Br,I])([SiX4]([F,Cl,Br,I])([F,Cl,Br,I])[F,Cl,Br,I])[F,Cl,Br,I])] silicon halide
[$([CH1](=O)CC[CH1]=O),$([CH1](=O)CCC[CH1]=O)] dialdehyde
S(=O)(=O)(OC)OC dialkyl sulphate
C[N+](C)=C1C=CCC=C1 bis(4-aminophenyl)methane salt
C=CC[F,Cl,Br,I] allyl halide
#
define $halo [F,Cl,Br,I]
[$(C([$halo])c1ccccc1);!$(C([$halo])([$halo])([$halo])c1ccccc1)] benzyl halide
#
[c,C][SX2,OX2]C(C)(C)C(=O)[OH1] fibrate
[$([F,Cl,Br,I]c1ccc([F,Cl,Br,I])cc1),$([F,Cl,Br,I]c1cc([F,Cl,Br,I])cc([F,Cl,Br,I])c1),$([F,Cl,Br,I]c1cc([F,Cl,Br,I])c([F,Cl,Br,I])cc1[F,Cl,Br,I]),$([F,Cl,Br,I]c1c([F,Cl,Br,I])c([F,Cl,Br,I])c([F,Cl,Br,I])c([F,Cl,Br,I])c1[F,Cl,Br,I])] polyhalogenated benzene
[$([F,Cl,Br,I]CC([F,Cl,Br,I])([F,Cl,Br,I])[F,Cl,Br,I]),$([F,Cl,Br,I]C([F,Cl,Br,I])C([F,Cl,Br,I])([F,Cl,Br,I])[F,Cl,Br,I]),$([F,Cl,Br,I]C([F,Cl,Br,I])([F,Cl,Br,I])C([F,Cl,Br,I])([F,Cl,Br,I])[F,Cl,Br,I])] polychlorinated ethane
[NX3](a)C(=O)C(=C)Br N-Aryl alpha-bromoacrylamide
[NX3]C(=O)N=NC(=O)[NX3] azodicarbonamide
CCC(c1ccc(O)cc1)=,-C(CC)c2ccc(O)cc2 4,4'-Dihydroxydiphenyl-ethane or -ethene
[F,Cl,Br,I]c1ccnnn1 halo-diazine or -triazine
O=C1NC(=O)c2ccccc12 thalidomide-type compound
[$(COC(=O)c1ccccc1C([OH1])=O),$(COC(=O)c1ccccc1C(OC)=O)] phthalate mono- or di-ester
C([F,Cl,Br,I])C(=O)C alpha-halo ketone
Oc1ccc(cc1N(=O)=O)N(=O)=O polynitrophenol or precursor
[$(Nc1ccc(cc1)-c2ccccc2),$(Nc1ccc(cc1)-c2ccc(N)cc2)] 4-aminodiphenyl benzidine naphthylamine or precursors
[$(C([F,Cl,Br,I])([F,Cl,Br,I])([F,Cl,Br,I])[CH1]=O),$(C([F,Cl,Br,I])([F,Cl,Br,I])([F,Cl,Br,I])C(O)O)] fluoroacetyl compound or fluoroacetate precursor
[$(S=C1NC=NN1),$(Nc1nnc[nH]1),$(S=C1NC=CN1)] 3-amino- or 3-mercapto-triazole or 2-Mercaptoimidazole
[$([c,C]C(=O)[F,Cl,Br,I]),$([c,C]S(=O)(=O)[F,Cl,Br,I]),$(P(=O)([Cl,Br,I])([Cl,Br,I])OC)] acid halide
S1[c,C]-[c,C]1 episulphide
[$([NX3]1[c,C]-[c,C]1),$([NX3]1C=C1)] azirine or aziridine
CC1CC(=O)O1 beta-lactone
[PX3](-O)(-O)OC alkyl ester of phosphoric or phosphonic acid
[$(C=CCC#N),$(C=CC#N)] alpha,beta-Unsaturated nitrile
SC(=S)[NX3;!R] thiuram disulphide or dithiocarbamate
C(=S)[NX3;!R] thiocarbamate
cC(=O)[NX3;!R][OH1] aromatic hydroxamic acid
C(Cl)(Cl)(Cl)c trichloromethyl aromatic compound
CC(C)(c1ccc([OH1])cc1)c2ccc([OH1])cc2 bisphenol or precursor
[$([C;!R]=[C;!R][C;!R]c1ccccc1),$([C;!R]#[C;!R][C;!R]c1ccccc1)] allylbenzene propargylbenzene or 1'-hydroxy derivatives
C(OCC1CO1)C2CO2
[C;!R]=[C;!R]O[c,C] enol ether
[$([F,Cl,Br,I]c1cc2Oc3cc([F,Cl,Br,I])c([F,Cl,Br,I])cc3Oc2cc1[F,Cl,Br,I]),$([F,Cl,Br,I]c1cc2oc3cc([F,Cl,Br,I])c([F,Cl,Br,I])cc3c2cc1[F,Cl,Br,I]),$([F,Cl,Br,I]c1ccc(cc1[F,Cl,Br,I])N=Nc2ccc([F,Cl,Br,I])c([F,Cl,Br,I])c2),$([F,Cl,Br,I]c1ccc(cc1[F,Cl,Br,I])N=N(=O)c2ccc([F,Cl,Br,I])c([F,Cl,Br,I])c2)] tetrachloro-dibenzodioxin -dibenzofuran -azobenzene -azoxybenzene or analogues
C([F,Cl,Br,I])OC alpha-halo ether
[NX3;!R]C(=O)OC=C vinyl carbamate
CNc1ncon1 3-aminomethyl-1,2,4-oxadiazole
[NX3;!R][CX4;!R][CX3;!R]=[CX3;!R] allylamine
[N;!R]C[OH] N-methylol derivatives
[NX3]Cl N-chloramines
#
# Nitro both forms
#
#define $no2A [N;+1](=O)[O;-1]
#define $no2B N(=O)=O
#[$([N;!R]a);!$(N(a)a);!$no2A;!$no2B] aromatic mono and dialkylamino groups
#
[N;!R]([CX4,#1])([CX4,#1])c1ccccc1 for aromatic amines
