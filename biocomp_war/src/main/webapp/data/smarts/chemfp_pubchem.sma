##############################################################################
# Adapted from http://code.google.com/p/chem-fingerprints/source/browse/chemfp/substruct.patterns (Andrew Dalke), downloaded 2011-12-05.
# 
#  NOTE: Some patterns omitted due to problems expressing efficiently as SMARTS.  E.g. ">= 32 C" can be expressed
#  but this can be very slow to match depending on implementation (if all matches enumerated).
# 
#  Jeremy Yang 
##############################################################################
#	[!H0].[!H0].[!H0].[!H0]
#	[!H0].[!H0].[!H0].[!H0].[!H0].[!H0].[!H0].[!H0]
#	[!H0].[!H0].[!H0].[!H0].[!H0].[!H0].[!H0].[!H0].[!H0].[!H0].[!H0].[!H0].[!H0].[!H0].[!H0].[!H0]
#	[!H0].[!H0].[!H0].[!H0].[!H0].[!H0].[!H0].[!H0].[!H0].[!H0].[!H0].[!H0].[!H0].[!H0].[!H0].[!H0].[!H0].[!H0].[!H0].[!H0].[!H0].[!H0].[!H0].[!H0].[!H0].[!H0].[!H0].[!H0].[!H0].[!H0].[!H0].[!H0]
[Li]
[Li].[Li]
[#5]
[#5].[#5] >= 2 B
[#5].[#5].[#5].[#5] >= 4 B
[#6].[#6] >= 2 C
[#6].[#6].[#6].[#6] >= 4 C
#	[#6].[#6].[#6].[#6].[#6].[#6].[#6].[#6] >= 8 C
#	[#6].[#6].[#6].[#6].[#6].[#6].[#6].[#6].[#6].[#6].[#6].[#6].[#6].[#6].[#6].[#6] >= 16 C
#	[#6].[#6].[#6].[#6].[#6].[#6].[#6].[#6].[#6].[#6].[#6].[#6].[#6].[#6].[#6].[#6].[#6].[#6].[#6].[#6].[#6].[#6].[#6].[#6].[#6].[#6].[#6].[#6].[#6].[#6].[#6].[#6] >= 32 C
[#7]
[#7].[#7]
[#7].[#7].[#7].[#7]
[#7].[#7].[#7].[#7].[#7].[#7].[#7].[#7]
[#8]
[#8].[#8] >= 2 O
[#8].[#8].[#8].[#8] >= 4 O
#	[#8].[#8].[#8].[#8].[#8].[#8].[#8].[#8] >= 8 O
#	[#8].[#8].[#8].[#8].[#8].[#8].[#8].[#8].[#8].[#8].[#8].[#8].[#8].[#8].[#8].[#8] >= 16 O
[F]
[F].[F] >= 2 F
[F].[F].[F].[F] >= 4 F
[Na]
[Na].[Na] >= 2 Na
[#14]
[#14].[#14] >= 2 Si
[#15]
[#15].[#15] >= 2 P
[#15].[#15].[#15].[#15] >= 4 P
[#16]
[#16].[#16] >= 2 S
[#16].[#16].[#16].[#16] >= 4 S
#	[#16].[#16].[#16].[#16].[#16].[#16].[#16].[#16] >= 8 S
[Cl]
[Cl].[Cl] >= 2 Cl
[Cl].[Cl].[Cl].[Cl] >= 4 Cl
#	[Cl].[Cl].[Cl].[Cl].[Cl].[Cl].[Cl].[Cl] >= 8 Cl
[K]
[K].[K] >= 2 K
[Br]
[Br].[Br] >= 2 Br
[Br].[Br].[Br].[Br] >= 4 Br
[I]
[I].[I] >= 2 I
[I].[I].[I].[I] >= 4 I
[Be] >= 1 Be
[Mg] >= 1 Mg
[Al] >= 1 Al
[Ca] >= 1 Ca
[Sc] >= 1 Sc
[Ti] >= 1 Ti
[V] >= 1 V
[Cr] >= 1 Cr
[Mn] >= 1 Mn
[Fe] >= 1 Fe
[Co] >= 1 Co
[Ni] >= 1 Ni
[Cu] >= 1 Cu
[Zn] >= 1 Zn
[Ga] >= 1 Ga
[#32] >= 1 Ge
[#33] >= 1 As
[#34] >= 1 Se
[Kr] >= 1 Kr
[Rb] >= 1 Rb
[Sr] >= 1 Sr
[Y] >= 1 Y
[Zr] >= 1 Zr
[Nb] >= 1 Nb
[Mo] >= 1 Mo
[Ru] >= 1 Ru
[Rh] >= 1 Rh
[Pd] >= 1 Pd
[Ag] >= 1 Ag
[Cd] >= 1 Cd
[In] >= 1 In
[Sn] >= 1 Sn
[Sb] >= 1 Sb
[#52] >= 1 Te
[Xe] >= 1 Xe
[Cs] >= 1 Cs
[Ba] >= 1 Ba
[Lu] >= 1 Lu
[Hf] >= 1 Hf
[Ta] >= 1 Ta
[W] >= 1 W
[Re] >= 1 Re
[Os] >= 1 Os
[Ir] >= 1 Ir
[Pt] >= 1 Pt
[Au] >= 1 Au
[Hg] >= 1 Hg
[Tl] >= 1 Tl
[Pb] >= 1 Pb
[Bi] >= 1 Bi
[La] >= 1 La
[Ce] >= 1 Ce
[Pr] >= 1 Pr
[Nd] >= 1 Nd
[Pm] >= 1 Pm
[Sm] >= 1 Sm
[Eu] >= 1 Eu
[Gd] >= 1 Gd
[Tb] >= 1 Tb
[Dy] >= 1 Dy
[Ho] >= 1 Ho
[Er] >= 1 Er
[Tm] >= 1 Tm
[Yb] >= 1 Yb
[Tc] >= 1 Tc
[U] >= 1 U

# Section 2: Rings in a canonic Extended Smallest Set of Smallest
# Rings (ESSSR) ring set - These bits test for the presence or count
# of the described chemical ring system.  An ESSSR ring is any ring
# which does not share three consecutive atoms with any other ring in
# the chemical structure.  For example, naphthalene has three ESSSR
# rings (two phenyl fragments and the 10-membered envelope), while
# biphenyl will yield a count of only two ESSSR rings.

*~1~*~*1 >= 1 any ring size 3
c1cc1 >= 1 aromatic carbon-only ring size 3
n1aa1 >= 1 aromatic nitrogen-containing ring size 3
[a;!#6]1aa1 >= 1 aromatic heteroatom-containing ring size 3
C~1~C~C1 >= 1 non-aromatic carbon-only ring size 3
N~1~A~A1 >= 1 non-aromatic nitrogen-containing ring size 3
[A;!#6]~1~A~A1 >= 1 non-aromatic heteroatom-containing ring size 3
*~1~*~*1 >= 2 any ring size 3
c1cc1 >= 2 aromatic carbon-only ring size 3
n1aa1 >= 2 aromatic nitrogen-containing ring size 3
[a;!#6]1aa1 >= 2 aromatic heteroatom-containing ring size 3
C~1~C~C1 >= 2 non-aromatic carbon-only ring size 3
N~1~A~A1 >= 2 non-aromatic nitrogen-containing ring size 3
[A;!#6]~1~A~A1 >= 2 non-aromatic heteroatom-containing ring size 3
*~1~*~*~*1 >= 1 any ring size 4
c1ccc1 >= 1 carbon-only ring size 4
n1aaa1 >= 1 nitrogen-containing ring size 4
[a;!#6]1aaa1 >= 1 heteroatom-containing ring size 4
C~1~C~C~C1 >= 1 non-aromatic carbon-only ring size 4
N~1~A~A~A1 >= 1 non-aromatic nitrogen-containing ring size 4
[A;!#6]~1~A~A~A1 >= 1 non-aromatic heteroatom-containing ring size 4
*~1~*~*~*1 >= 2 any ring size 4
c1ccc1 >= 2 aromatic carbon-only ring size 4
n1aaa1 >= 2 aromatic nitrogen-containing ring size 4
[a;!#6]1aaa1 >= 2 aromatic heteroatom-containing ring size 4
C~1~C~C~C1 >= 2 non-aromatic carbon-only ring size 4
N~1~A~A~A1 >= 2 non-aromatic nitrogen-containing ring size 4
[A;!#6]~1~A~A~A1 >= 2 non-aromatic heteroatom-containing ring size 4
*~1~*~*~*~*1 >= 1 any ring size 5
c1cccc1 >= 1 aromatic carbon-only ring size 5
n1aaaa1 >= 1 aromatic nitrogen-containing ring size 5
[a;!#6]1aaaa1 >= 1 aromatic heteroatom-containing ring size 5
C~1~C~C~C~C1 >= 1 non-aromatic carbon-only ring size 5
N~1~A~A~A~A1 >= 1 non-aromatic nitrogen-containing ring size 5
[A;!#6]~1A~A~A~A1 >= 1 non-aromatic heteroatom-containing ring size 5
*~1~*~*~*~*1 >= 2 any ring size 5
c1cccc1 >= 2 aromatic carbon-only ring size 5
n1aaaa1 >= 2 aromatic nitrogen-containing ring size 5
[a;!#6]1aaaa1 >= 2 aromatic heteroatom-containing ring size 5
C~1~C~C~C~C1 >= 2 non-aromatic carbon-only ring size 5
N~1~A~A~A~A1 >= 2 non-aromatic nitrogen-containing ring size 5
[A;!#6]~1A~A~A~A1 >= 2 non-aromatic heteroatom-containing ring size 5
*~1~*~*~*~*1 >= 3 any ring size 5
c1cccc1 >= 3 aromatic carbon-only ring size 5
n1aaaa1 >= 3 aromatic nitrogen-containing ring size 5
[a;!#6]1aaaa1 >= 3 aromatic heteroatom-containing ring size 5
C~1~C~C~C~C1 >= 3 non-aromatic carbon-only ring size 5
N~1~A~A~A~A1 >= 3 non-aromatic nitrogen-containing ring size 5
[A;!#6]~1A~A~A~A1 >= 3 non-aromatic heteroatom-containing ring size 5
*~1~*~*~*~*1 >= 4 any ring size 5
c1cccc1 >= 4 aromatic carbon-only ring size 5
n1aaaa1 >= 4 aromatic nitrogen-containing ring size 5
[a;!#6]1aaaa1 >= 4 aromatic heteroatom-containing ring size 5
C~1~C~C~C~C1 >= 4 non-aromatic carbon-only ring size 5
N~1~A~A~A~A1 >= 4 non-aromatic nitrogen-containing ring size 5
[A;!#6]~1A~A~A~A1 >= 4 non-aromatic heteroatom-containing ring size 5
*~1~*~*~*~*1 >= 5 any ring size 5
c1cccc1 >= 5 aromatic carbon-only ring size 5
n1aaaa1 >= 5 aromatic nitrogen-containing ring size 5
[a;!#6]1aaaa1 >= 5 aromatic heteroatom-containing ring size 5
C~1~C~C~C~C1 >= 5 non-aromatic carbon-only ring size 5
N~1~A~A~A~A1 >= 5 non-aromatic nitrogen-containing ring size 5
[A;!#6]~1A~A~A~A1 >= 5 non-aromatic heteroatom-containing ring size 5

# Rings of size 6

*~1~*~*~*~*~*1 >= 1 any ring size 6
c1ccccc1 >= 1 aromatic carbon-only ring size 6
n1aaaaa1 >= 1 aromatic nitrogen-containing ring size 6
[a;!#6]1aaaaa1 >= 1 aromatic heteroatom-containing ring size 6
C~1~C~C~C~C~C1 >= 1 non-aromatic carbon-only ring size 6
N~1~A~A~A~A~A1 >= 1 non-aromatic nitrogen-containing ring size 6
[A;!#6]~1~A~A~A~A~A1 >= 1 non-aromatic heteroatom-containing ring size 6
*~1~*~*~*~*~*1 >= 2 any ring size 6
c1ccccc1 >= 2 aromatic carbon-only ring size 6
n1aaaaa1 >= 2 aromatic nitrogen-containing ring size 6
[a;!#6]1aaaaa1 >= 2 aromatic heteroatom-containing ring size 6
C~1~C~C~C~C~C1 >= 2 non-aromatic carbon-only ring size 6
N~1~A~A~A~A~A1 >= 2 non-aromatic nitrogen-containing ring size 6
[A;!#6]~1~A~A~A~A~A1 >= 2 non-aromatic heteroatom-containing ring size 6
*~1~*~*~*~*~*1 >= 3 any ring size 6
c1ccccc1 >= 3 aromatic carbon-only ring size 6
n1aaaaa1 >= 3 aromatic nitrogen-containing ring size 6
[a;!#6]1aaaaa1 >= 3 aromatic heteroatom-containing ring size 6
C~1~C~C~C~C~C1 >= 3 non-aromatic carbon-only ring size 6
N~1~A~A~A~A~A1 >= 3 non-aromatic nitrogen-containing ring size 6
[A;!#6]~1~A~A~A~A~A1 >= 3 non-aromatic heteroatom-containing ring size 6
*~1~*~*~*~*~*1 >= 4 any ring size 6
c1ccccc1 >= 4 aromatic carbon-only ring size 6
n1aaaaa1 >= 4 aromatic nitrogen-containing ring size 6
[a;!#6]1aaaaa1 >= 4 aromatic heteroatom-containing ring size 6
C~1~C~C~C~C~C1 >= 4 non-aromatic carbon-only ring size 6
N~1~A~A~A~A~A1 >= 4 non-aromatic nitrogen-containing ring size 6
[A;!#6]~1~A~A~A~A~A1 >= 4 non-aromatic heteroatom-containing ring size 6
*~1~*~*~*~*~*1 >= 5 any ring size 6
c1ccccc1 >= 5 aromatic carbon-only ring size 6
n1aaaaa1 >= 5 aromatic nitrogen-containing ring size 6
[a;!#6]1aaaaa1 >= 5 aromatic heteroatom-containing ring size 6
C~1~C~C~C~C~C1 >= 5 non-aromatic carbon-only ring size 6
N~1~A~A~A~A~A1 >= 5 non-aromatic nitrogen-containing ring size 6
[A;!#6]~1~A~A~A~A~A1 >= 5 non-aromatic heteroatom-containing ring size 6

# Rings of size 7

*~1~*~*~*~*~*~*1 >= 1 any ring size 7
c1cccccc1 >= 1 aromatic carbon-only ring size 7
n1aaaaaa1 >= 1 aromatic nitrogen-containing ring size 7
[a;!#6]1aaaaaa1 >= 1 aromatic heteroatom-containing ring size 7
C~1~C~C~C~C~C~C1 >= 1 non-aromatic carbon-only ring size 7
N~1~A~A~A~A~A~A1 >= 1 non-aromatic nitrogen-containing ring size 7
[A;!#6]~1~A~A~A~A~A~A1 >= 1 non-aromatic heteroatom-containing ring size 7
*~1~*~*~*~*~*~*1 >= 2 any ring size 7
c1cccccc1 >= 2 aromatic carbon-only ring size 7
n1aaaaaa1 >= 2 aromatic nitrogen-containing ring size 7
[a;!#6]1aaaaaa1 >= 2 aromatic heteroatom-containing ring size 7
C~1~C~C~C~C~C~C1 >= 2 non-aromatic carbon-only ring size 7
N~1~A~A~A~A~A~A1 >= 2 non-aromatic nitrogen-containing ring size 7
[A;!#6]~1~A~A~A~A~A~A1 >= 2 non-aromatic heteroatom-containing ring size 7

# Rings of size 8

*~1~*~*~*~*~*~*~*1 >= 1 any ring size 8
c1ccccccc1 >= 1 aromatic carbon-only ring size 8
n1aaaaaaa1 >= 1 aromatic nitrogen-containing ring size 8
[a;!#6]1aaaaaaa1 >= 1 aromatic heteroatom-containing ring size 8
C~1~C~C~C~C~C~C~C1 >= 1 non-aromatic carbon-only ring size 8
N~1~A~A~A~A~A~A~A1 >= 1 non-aromatic nitrogen-containing ring size 8
[A;!#6]~1~A~A~A~A~A~A~A1 >= 1 non-aromatic heteroatom-containing ring size 8
*~1~*~*~*~*~*~*~*1 >= 2 any ring size 8
c1ccccccc1 >= 2 aromatic carbon-only ring size 8
n1aaaaaaa1 >= 2 aromatic nitrogen-containing ring size 8
[a;!#6]1aaaaaaa1 >= 2 aromatic heteroatom-containing ring size 8
C~1~C~C~C~C~C~C~C1 >= 2 non-aromatic carbon-only ring size 8
N~1~A~A~A~A~A~A~A1 >= 2 non-aromatic nitrogen-containing ring size 8
[A;!#6]~1~A~A~A~A~A~A~A1 >= 2 non-aromatic heteroatom-containing ring size 8

# Rings of size 9

*~1~*~*~*~*~*~*~*~*1 >= 1 any ring size 9
c1cccccccc1 >= 1 aromatic carbon-only ring size 9
n1aaaaaaaa1 >= 1 aromatic nitrogen-containing ring size 9
[a;!#6]1aaaaaaaa1 >= 1 aromatic heteroatom-containing ring size 9
C~1~C~C~C~C~C~C~C~C1 >= 1 non-aromatic carbon-only ring size 9
N~1~A~A~A~A~A~A~A~A1 >= 1 non-aromatic nitrogen-containing ring size 9
[A;!#6]~1~A~A~A~A~A~A~A~A1 >= 1 non-aromatic heteroatom-containing ring size 9

# Rings of size 10

*~1~*~*~*~*~*~*~*~*~*1 >= 1 any ring size 10
c1ccccccccc1 >= 1 aromatic carbon-only ring size 10
n1aaaaaaaaa1 >= 1 aromatic nitrogen-containing ring size 10
[a;!#6]1aaaaaaaaa1 >= 1 aromatic heteroatom-containing ring size 10
C~1~C~C~C~C~C~C~C~C~C1 >= 1 non-aromatic carbon-only ring size 10
N~1~A~A~A~A~A~A~A~A~A1 >= 1 non-aromatic nitrogen-containing ring size 10
[A;!#6]~1~A~A~A~A~A~A~A~A~A1 >= 1 non-aromatic heteroatom-containing ring size 10

[aR] >= 1 aromatic ring
# Interesting. RDkit doesn't support [aR!#6]. Is that a legal SMARTS?
[aR;!#6] >= 1 hetero-aromatic ring
#	<aromatic-rings> >= 2 aromatic rings
#	<hetero-aromatic-rings> >= 2 hetero-aromatic rings
#	<aromatic-rings> >= 3 aromatic rings
#	<hetero-aromatic-rings> >= 3 hetero-aromatic rings
#	<aromatic-rings> >= 4 aromatic rings
#	<hetero-aromatic-rings> >= 4 hetero-aromatic rings

# Section 3: Simple atom pairs - These bits test for the presence of
# patterns of bonded atom pairs, regardless of bond order or count.

[Li;!H0]    Li-H
[Li]~[Li]    Li-Li
[Li]~[#5]    Li-B
[Li]~[#6]    Li-C
[Li]~[#8]    Li-O
[Li]~[F]    Li-F
[Li]~[#15]    Li-P
[Li]~[#16]    Li-S
[Li]~[Cl]    Li-Cl
[#5;!H0]    B-H
[#5]~[#5]    B-B
[#5]~[#6]    B-C
[#5]~[#7]    B-N
[#5]~[#8]    B-O
[#5]~[F]    B-F
[#5]~[#6]    B-Si
[#5]~[#15]    B-P
[#5]~[#16]    B-S
[#5]~[Cl]    B-Cl
[#5]~[Br]    B-Br
[#6;!H0]    C-H
[#6]~[#6]    C-C
[#6]~[#7]    C-N
[#6]~[#8]    C-O
[#6]~[F]    C-F
[#6]~[Na]    C-Na
[#6]~[Mg]    C-Mg
[#6]~[Al]    C-Al
[#6]~[#14]    C-Si
[#6]~[#15]    C-P
[#6]~[#16]    C-S
[#6]~[Cl]    C-Cl
[#6]~[#33]    C-As
[#6]~[#34]    C-Se
[#6]~[Br]    C-Br
[#6]~[I]    C-I
[#7;!H0]    N-H
[#7]~[#7]    N-N
[#7]~[#8]    N-O
[#7]~[F]    N-F
[#7]~[#14]    N-Si
[#7]~[#15]    N-P
[#7]~[#16]    N-S
[#7]~[Cl]    N-Cl
[#7]~[Br]    N-Br
[#8;!H0]    O-H
[#8]~[#8]    O-O
[#8]~[Mg]    O-Mg
[#8]~[Na]    O-Na
[#8]~[Al]    O-Al
[#8]~[#14]    O-Si
[#8]~[#15]    O-P
[#8]~[K]    O-K
[F]~[#15]    F-P
[F]~[#16]    F-S
[Al;!H0]    Al-H
[Al]~[Cl]    Al-Cl
[#14;!H0]    Si-H
[#14]~[#14]    Si-Si
[#14]~[Cl]    Si-Cl
[#15;!H0]    P-H
[#15]~[#15]    P-P
[#33;!H0]    As-H
[#33]~[#33]    As-As


# Section 4: Simple atom nearest neighbors - These bits test for the
# presence of atom nearest neighbor patterns, regardless of bond order
# (denoted by "~") or count, but where bond aromaticity (denoted by
# ":") is significant.

[#6](~Br)(~[#6])    C(~Br)(~C)
[#6](~Br)(~[#6])(~[#6])    C(~Br)(~C)(~C)
[#6;!H0](~Br)    C(~Br)(~H)
c(~Br)(:c)    C(~Br)(:C)
c(~Br)(:n)    C(~Br)(:N)
[#6](~[#6])(~[#6])    C(~C)(~C)
[#6](~[#6])(~[#6])(~[#6])    C(~C)(~C)(~C)
[#6](~[#6])(~[#6])(~[#6])(~[#6])    C(~C)(~C)(~C)(~C)
[#6;!H0](~[#6])(~[#6])(~[#6])    C(~C)(~C)(~C)(~H)
[#6](~[#6])(~[#6])(~[#6])(~[#7])    C(~C)(~C)(~C)(~N)
[#6](~[#6])(~[#6])(~[#6])(~[#8])    C(~C)(~C)(~C)(~O)
[#6;!H0](~[#6])(~[#6])(~[#7])    C(~C)(~C)(~H)(~N)
[#6;!H0](~[#6])(~[#6])(~[#8])    C(~C)(~C)(~H)(~O)
[#6](~[#6])(~[#6])(~[#7])    C(~C)(~C)(~N)
[#6](~[#6])(~[#6])(~[#8])    C(~C)(~C)(~O)
[#6](~[#6])(~Cl)    C(~C)(~Cl)
[#6;!H0](~[#6])(~Cl)    C(~C)(~Cl)(~H)
[#6;!H0](~[#6])    C(~C)(~H)
[#6;!H0](~[#6])(~[#7])    C(~C)(~H)(~N)
[#6;!H0](~[#6])(~[#8])    C(~C)(~H)(~O)
[#6;!H0](~[#6])(~[#8])(~[#8])    C(~C)(~H)(~O)(~O)
[#6;!H0](~[#6])(~[#15])    C(~C)(~H)(~P)
[#6;!H0](~[#6])(~[#16])    C(~C)(~H)(~S)
[#6](~[#6])(~I)    C(~C)(~I)
[#6](~[#6])(~[#7])    C(~C)(~N)
[#6](~[#6])(~[#8])    C(~C)(~O)
[#6](~[#6])(~[#16])    C(~C)(~S)
[#6](~[#6])(~[#14])    C(~C)(~Si)
c(~[#6])(:c)    C(~C)(:C)
c(~[#6])(:c)(:c)    C(~C)(:C)(:C)
c(~[#6])(:c)(:n)    C(~C)(:C)(:N)
c(~[#6])(:n)    C(~C)(:N)
c(~[#6])(:n)(:n)    C(~C)(:N)(:N)
[#6](~Cl)(~Cl)    C(~Cl)(~Cl)
[#6;!H0](~Cl)    C(~Cl)(~H)
c(~Cl)(:c)    C(~Cl)(:C)
[#6](~F)(~F)    C(~F)(~F)
c(~F)(:c)    C(~F)(:C)
[#6;!H0](~[#7])    C(~H)(~N)
[#6;!H0](~[#8])    C(~H)(~O)
[#6;!H0](~[#8])(~[#8])    C(~H)(~O)(~O)
[#6;!H0](~[#16])    C(~H)(~S)
[#6;!H0](~[#14])    C(~H)(~Si)
[c;!H0](:c)    C(~H)(:C)
[c;!H0](:c)(:c)    C(~H)(:C)(:C)
[c;!H0](:c)(:n)    C(~H)(:C)(:N)
[c;!H0](:n)    C(~H)(:N)
[#6;!H0;!H1;!H2]    C(~H)(~H)(~H)
[#6](~[#7])(~[#7])    C(~N)(~N)
c(~[#7])(:c)    C(~N)(:C)
c(~[#7])(:c)(:c)    C(~N)(:C)(:C)
c(~[#7])(:c)(:n)    C(~N)(:C)(:N)
c(~[#7])(:n)    C(~N)(:N)
[#6](~[#8])(~[#8])    C(~O)(~O)
c(~[#8])(:c)    C(~O)(:C)
c(~[#8])(:c)(:c)    C(~O)(:C)(:C)
c(~[#16])(:c)    C(~S)(:C)
c(:c)(:c)    C(:C)(:C)
c(:c)(:c)(:c)    C(:C)(:C)(:C)
c(:c)(:c)(:n)    C(:C)(:C)(:N)
c(:c)(:n)    C(:C)(:N)
c(:c)(:n)(:n)    C(:C)(:N)(:N)
c(:n)(:n)    C(:N)(:N)
[#7](~[#6])(~[#6])    N(~C)(~C)
[#7](~[#6])(~[#6])(~[#6])    N(~C)(~C)(~C)
[#7;!H0](~[#6])(~[#6])    N(~C)(~C)(~H)
[#7;!H0](~[#6])    N(~C)(~H)
[#7;!H0](~[#6])(~[#7])    N(~C)(~H)(~N)
[#7](~[#6])(~[#8])    N(~C)(~O)
n(~[#6])(:c)    N(~C)(:C)
n(~[#6])(:c)(:c)    N(~C)(:C)(:C)
[#7;!H0](~[#7])    N(~H)(~N)
[n;!H0](:c)    N(~H)(:C)
[n;!H0](:c)(:c)    N(~H)(:C)(:C)
[#7](~[#8])(~[#8])    N(~O)(~O)
n(~[#8])(:o)    N(~O)(:O)
n(:c)(:c)    N(:C)(:C)
n(:c)(:c)(:c)    N(:C)(:C)(:C)
[#8](~[#6])(~[#6])    O(~C)(~C)
[#8;!H0](~[#6])    O(~C)(~H)
[#8](~[#6])(~[#15])    O(~C)(~P)
[#8;!H0](~[#16])    O(~H)(~S)
o(:c)(:c)    O(:C)(:C)
[#15](~[#6])(~[#6])    P(~C)(~C)
[#15](~[#8])(~[#8])    P(~O)(~O)
[#16](~[#6])(~[#6])    S(~C)(~C)
[#16;!H0](~[#6])    S(~C)(~H)
[#16](~[#6])(~[#8])    S(~C)(~O)
[#14](~[#6])(~[#6])    Si(~C)(~C)


# Section 5: Detailed atom neighborhoods - These bits test for the
# presence of detailed atom neighborhood patterns, regardless of
# count, but where bond orders are specific, bond aromaticity matches
# both single and double bonds, and where "-", "=", and "#" matches a
# single bond, double bond, and triple bond order, respectively.

[#6]=,:[#6]   C=C
[#6]#[#6]   C#C
[#6]=,:[#7]   C=N
[#6]#[#7]   C#N
[#6]=,:[#8]   C=O
[#6]=,:[s,S]   C=S
[#7]=,:[#7]   N=N
[#7]=,:[#8]   N=O
[#7]=,:[#15]   N=P
[#15]=,:[#8]   P=O
[#15]=,:[#15]   P=P
[#6]#[#6]-,:[#6]   C(#C)(-C)
[#6;!H0]#[#6]   C(#C)(-H)
[#7]#[#6]-,:[#6]   C(#N)(-C)
[#6](-,:[#6])(-,:[#6])(=,:[#6])   C(-C)(-C)(=C)
[#6](-,:[#6])(-,:[#6])(=,:[#7])   C(-C)(-C)(=N)
[#6](-,:[#6])(-,:[#6])(=,:[#8])   C(-C)(-C)(=O)
[#6](-,:[#6])(Cl)(=,:[#8])   C(-C)(-Cl)(=O)
[#6&!H0](-,:[#6])(=,:[#6])   C(-C)(-H)(=C)
[#6&!H0](-,:[#6])(=,:[#7])   C(-C)(-H)(=N)
[#6&!H0](-,:[#6])(=,:[#8])   C(-C)(-H)(=O)
[#6](-,:[#6])(-,:[#7])(=,:[#6])   C(-C)(-N)(=C)
[#6](-,:[#6])(-,:[#7])(=,:[#7])   C(-C)(-N)(=N)
[#6](-,:[#6])(-,:[#7])(=,:[#8])   C(-C)(-N)(=O)
[#6](-,:[#6])(-,:[#8])(=,:[#8])   C(-C)(-O)(=O)
[#6](-,:[#6])(=,:[#6])   C(-C)(=C)
[#6](-,:[#6])(=,:[#7])   C(-C)(=N)
[#6](-,:[#6])(=,:[#8])   C(-C)(=O)
[#6](Cl)(=,:[#8])   C(-Cl)(=O)
[#6;!H0](-,:[#7])(=,:[#6])   C(-H)(-N)(=C)
[#6;!H0](=,:[#6])   C(-H)(=C)
[#6;!H0](=,:[#7])   C(-H)(=N)
[#6;!H0](=,:[#8])   C(-H)(=O)
[#6](-,:[#7])(=,:[#6])   C(-N)(=C)
[#6](-,:[#7])(=,:[#7])   C(-N)(=N)
[#6](-,:[#7])(=,:[#8])   C(-N)(=O)
[#6](-,:[#8])(=,:[#8])   C(-O)(=O)
[#7](-,:[#6])(=,:[#6])   N(-C)(=C)
[#7](-,:[#6])(=,:[#8])   N(-C)(=O)
[#7](-,:[#8])(=,:[#8])   N(-O)(=O)
[#15](-,:[#8])(=,:[#8])   P(-O)(=O)
[#16](-,:[#6])(=,:[#8])   S(-C)(=O)
[#16](-,:[#8])(=,:[#8])   S(-O)(=O)
[#16](=,:[#8])(=,:[#8])   S(=O)(=O)

# Section 6: Simple SMARTS patterns - These bits test for the presence
# of simple SMARTS patterns, regardless of count, but where bond
# orders are specific and bond aromaticity matches both single and
# double bonds.

[#6]-,:[#6]-,:[#6]#C   C-C-C#C
[#8]-,:[#6]-,:[#6]=,:[#7]   O-C-C=N
[#8]-,:[#6]-,:[#6]=,:[#8]   O-C-C=O
n:c-,:[#16;!H0]   N:C-S-[#1]
[#7]-,:[#6]-,:[#6]=,:[#6]   N-C-C=C
[#8]=,:[#16]-,:[#6]-,:[#6]   O=S-C-C
N#[#6]-,:[#6]=,:[#6]   N#C-C=C
[#6]=,:[#7]-,:[#7]-,:[#6]   C=N-N-C
[#8]=,:[#16]-,:[#6]-,:[#7]   O=S-C-N
[#16]-,:[#16]-,:c:c   S-S-C:C
c:c-,:[#6]=,:[#6]   C:C-C=C
s:c:c:c   S:C:C:C
c:n:c-,:[#6]   C:N:C-C
[#16]-,:c:n:c   S-C:N:C
s:c:c:n   S:C:C:N
[#16]-,:[#6]=,:[#7]-,:[#6]   S-C=N-C
[#6]-,:[#8]-,:[#6]=,:[#6]   C-O-C=C
[#7]-,:[#7]-,:c:c   N-N-C:C
[#16]-,:[#6]=,:[#7;!H0]   S-C=N-[#1]
[#16]-,:[#6]-,:[#16]-,:[#6]   S-C-S-C
c:s:c-,:[#6]   C:S:C-C
[#8]-,:[#16]-,:c:c   O-S-C:C
c:n-,:c:c   C:N-C:C
[#7]-,:[#16]-,:c:c   N-S-C:C
[#7]-,:c:n:c   N-C:N:C
n:c:c:n   N:C:C:N
[#7]-,:c:n:n   N-C:N:N
[#7]-,:[#6]=,:[#7]-,:[#6]   N-C=N-C
[#7]-,:[#6]=,:[#7;!H0]   N-C=N-[#1]
[#7]-,:[#6]-,:[#16]-,:[#6]   N-C-S-C
[#6]-,:[#6]-,:[#6]=,:[#6]   C-C-C=C
[#6]-,:n:[c;!H0]   C-N:C-[#1]
[#7]-,:c:o:c   N-C:O:C
[#8]=,:[#6]-,:c:c   O=C-C:C
[#8]=,:[#6]-,:c:n   O=C-C:N
[#6]-,:[#7]-,:c:c   C-N-C:C
n:n-,:[#6;!H0]   N:N-C-[#1]
[#8]-,:c:c:n   O-C:C:N
[#8]-,:[#6]=,:[#6]-,:[#6]   O-C=C-C
[#7]-,:c:c:n   N-C:C:N
[#6]-,:[#16]-,:c:c   C-S-C:C
Cl-,:c:c-,:[#6]   Cl-C:C-C
[#7]-,:[#6]=,:[#6;!H0]   N-C=C-[#1]
Cl-,:c:[c;!H0]   Cl-C:C-[#1]
n:c:n-,:[#6]   N:C:N-C
Cl-,:c:c-,:[#8]   Cl-C:C-O
[#6]-,:c:n:c   C-C:N:C
[#6]-,:[#6]-,:[#16]-,:[#6]   C-C-S-C
[#16]=,:[#6]-,:[#7]-,:[#6]   S=C-N-C
Br-,:c:c-,:[#6]   Br-C:C-C
[#7;!H0]-,:[#7;!H0]   [#1]-N-N-[#1]
[#16]=,:[#6]-,:[#7;!H0]   S=C-N-[#1]
[#6]-,:[#33]-,:[#8;!H0]   C-[As]-O-[#1]
s:c:[c;!H0]   S:C:C-[#1]
[#8]-,:[#7]-,:[#6]-,:[#6]   O-N-C-C
[#7]-,:[#7]-,:[#6]-,:[#6]   N-N-C-C
[#6;!H0]=,:[#6;!H0]   [#1]-C=C-[#1]
[#7]-,:[#7]-,:[#6]-,:[#7]   N-N-C-N
[#8]=,:[#6]-,:[#7]-,:[#7]   O=C-N-N
[#7]=,:[#6]-,:[#7]-,:[#6]   N=C-N-C
[#6]=,:[#6]-,:c:c   C=C-C:C
c:n-,:[#6;!H0]   C:N-C-[#1]
[#6]-,:[#7]-,:[#7;!H0]   C-N-N-[#1]
n:c:c-,:[#6]   N:C:C-C
[#6]-,:[#6]=,:[#6]-,:[#6]   C-C=C-C
[#33]-,:c:[c;!H0]   [As]-C:C-[#1]
Cl-,:c:c-,:Cl   Cl-C:C-Cl
c:c:[n;!H0]   C:C:N-[#1]
[#7;!H0]-,:[#6;!H0]   [#1]-N-C-[#1]
Cl-,:[#6]-,:[#6]-,:Cl   Cl-C-C-Cl
n:c-,:c:c   N:C-C:C
[#16]-,:c:c-,:[#6]   S-C:C-C
[#16]-,:c:[c;!H0]   S-C:C-[#1]
[#16]-,:c:c-,:[#7]   S-C:C-N
[#16]-,:c:c-,:[#8]   S-C:C-O
[#8]=,:[#6]-,:[#6]-,:[#6]   O=C-C-C
[#8]=,:[#6]-,:[#6]-,:[#7]   O=C-C-N
[#8]=,:[#6]-,:[#6]-,:[#8]   O=C-C-O
[#7]=,:[#6]-,:[#6]-,:[#6]   N=C-C-C
[#7]=,:[#6]-,:[#6;!H0]   N=C-C-[#1]
[#6]-,:[#7]-,:[#6;!H0]   C-N-C-[#1]
[#8]-,:c:c-,:[#6]   O-C:C-C
[#8]-,:c:[c;!H0]   O-C:C-[#1]
[#8]-,:c:c-,:[#7]   O-C:C-N
[#8]-,:c:c-,:[#8]   O-C:C-O
[#7]-,:c:c-,:[#6]   N-C:C-C
[#7]-,:c:[c;!H0]   N-C:C-[#1]
[#7]-,:c:c-,:[#7]   N-C:C-N
[#8]-,:[#6]-,:c:c   O-C-C:C
[#7]-,:[#6]-,:c:c   N-C-C:C
Cl-,:[#6]-,:[#6]-,:[#6]   Cl-C-C-C
Cl-,:[#6]-,:[#6]-,:[#8]   Cl-C-C-O
c:c-,:c:c   C:C-C:C
[#8]=,:[#6]-,:[#6]=,:[#6]   O=C-C=C
Br-,:[#6]-,:[#6]-,:[#6]   Br-C-C-C
[#7]=,:[#6]-,:[#6]=,:[#6]   N=C-C=C
[#6]=,:[#6]-,:[#6]-,:[#6]   C=C-C-C
n:c-,:[#8;!H0]   N:C-O-[#1]
[#8]=,:[#7]-,:c:c   O=N-C:C
[#8]-,:[#6]-,:[#7;!H0]   O-C-N-[#1]
[#7]-,:[#6]-,:[#7]-,:[#6]   N-C-N-C
Cl-,:[#6]-,:[#6]=,:[#8]   Cl-C-C=O
Br-,:[#6]-,:[#6]=,:[#8]   Br-C-C=O
[#8]-,:[#6]-,:[#8]-,:[#6]   O-C-O-C
[#6]=,:[#6]-,:[#6]=,:[#6]   C=C-C=C
c:c-,:[#8]-,:[#6]   C:C-O-C
[#8]-,:[#6]-,:[#6]-,:[#7]   O-C-C-N
[#8]-,:[#6]-,:[#6]-,:[#8]   O-C-C-O
N#[#6]-,:[#6]-,:[#6]   N#C-C-C
[#7]-,:[#6]-,:[#6]-,:[#7]   N-C-C-N
c:c-,:[#6]-,:[#6]   C:C-C-C
[#6;!H0]-,:[#8;!H0]   [#1]-C-O-[#1]
n:c:n:c   N:C:N:C
[#8]-,:[#6]-,:[#6]=,:[#6]   O-C-C=C
[#8]-,:[#6]-,:c:c-,:[#6]   O-C-C:C-C
[#8]-,:[#6]-,:c:c-,:[#8]   O-C-C:C-O
[#7]=,:[#6]-,:c:[c;!H0]   N=C-C:C-[#1]
c:c-,:[#7]-,:c:c   C:C-N-C:C
[#6]-,:c:c-,:c:c   C-C:C-C:C
[#8]=,:[#6]-,:[#6]-,:[#6]-,:[#6]   O=C-C-C-C
[#8]=,:[#6]-,:[#6]-,:[#6]-,:[#7]   O=C-C-C-N
[#8]=,:[#6]-,:[#6]-,:[#6]-,:[#8]   O=C-C-C-O
[#6]-,:[#6]-,:[#6]-,:[#6]-,:[#6]   C-C-C-C-C
Cl-,:c:c-,:[#8]-,:[#6]   Cl-C:C-O-C
c:c-,:[#6]=,:[#6]-,:[#6]   C:C-C=C-C
[#6]-,:c:c-,:[#7]-,:[#6]   C-C:C-N-C
[#6]-,:[#16]-,:[#6]-,:[#6]-,:[#6]   C-S-C-C-C
[#7]-,:c:c-,:[#8;!H0]   N-C:C-O-[#1]
[#8]=,:[#6]-,:[#6]-,:[#6]=,:[#8]   O=C-C-C=O
[#6]-,:c:c-,:[#8]-,:[#6]   C-C:C-O-C
[#6]-,:c:c-,:[#8;!H0]   C-C:C-O-[#1]
Cl-,:[#6]-,:[#6]-,:[#6]-,:[#6]   Cl-C-C-C-C
[#7]-,:[#6]-,:[#6]-,:[#6]-,:[#6]   N-C-C-C-C
[#7]-,:[#6]-,:[#6]-,:[#6]-,:[#7]   N-C-C-C-N
[#6]-,:[#8]-,:[#6]-,:[#6]=,:[#6]   C-O-C-C=C
c:c-,:[#6]-,:[#6]-,:[#6]   C:C-C-C-C
[#7]=,:[#6]-,:[#7]-,:[#6]-,:[#6]   N=C-N-C-C
[#8]=,:[#6]-,:[#6]-,:c:c   O=C-C-C:C
Cl-,:c:c:c-,:[#6]   Cl-C:C:C-C
[#6;!H0]-,:[#6]=,:[#6;!H0]   [#1]-C-C=C-[#1]
[#7]-,:c:c:c-,:[#6]   N-C:C:C-C
[#7]-,:c:c:c-,:[#7]   N-C:C:C-N
[#8]=,:[#6]-,:[#6]-,:[#7]-,:[#6]   O=C-C-N-C
[#6]-,:c:c:c-,:[#6]   C-C:C:C-C
[#6]-,:[#8]-,:[#6]-,:c:c   C-O-C-C:C
[#8]=,:[#6]-,:[#6]-,:[#8]-,:[#6]   O=C-C-O-C
[#8]-,:c:c-,:[#6]-,:[#6]   O-C:C-C-C
[#7]-,:[#6]-,:[#6]-,:c:c   N-C-C-C:C
[#6]-,:[#6]-,:[#6]-,:c:c   C-C-C-C:C
Cl-,:[#6]-,:[#6]-,:[#7]-,:[#6]   Cl-C-C-N-C
[#6]-,:[#8]-,:[#6]-,:[#8]-,:[#6]   C-O-C-O-C
[#7]-,:[#6]-,:[#6]-,:[#7]-,:[#6]   N-C-C-N-C
[#7]-,:[#6]-,:[#8]-,:[#6]-,:[#6]   N-C-O-C-C
[#6]-,:[#7]-,:[#6]-,:[#6]-,:[#6]   C-N-C-C-C
[#6]-,:[#6]-,:[#8]-,:[#6]-,:[#6]   C-C-O-C-C
[#7]-,:[#6]-,:[#6]-,:[#8]-,:[#6]   N-C-C-O-C
c:c:n:n:c   C:C:N:N:C
[#6]-,:[#6]-,:[#6]-,:[#8;!H0]   C-C-C-O-[#1]
c:c-,:[#6]-,:c:c   C:C-C-C:C
[#8]-,:[#6]-,:[#6]=,:[#6]-,:[#6]   O-C-C=C-C
c:c-,:[#8]-,:[#6]-,:[#6]   C:C-O-C-C
[#7]-,:c:c:c:n   N-C:C:C:N
[#8]=,:[#6]-,:[#8]-,:c:c   O=C-O-C:C
[#8]=,:[#6]-,:c:c-,:[#6]   O=C-C:C-C
[#8]=,:[#6]-,:c:c-,:[#7]   O=C-C:C-N
[#8]=,:[#6]-,:c:c-,:[#8]   O=C-C:C-O
[#6]-,:[#8]-,:c:c-,:[#6]   C-O-C:C-C
[#8]=,:[#33]-,:c:c:c   O=[As]-C:C:C
[#6]-,:[#7]-,:[#6]-,:c:c   C-N-C-C:C
[#16]-,:c:c:c-,:[#7]   S-C:C:C-N
[#8]-,:c:c-,:[#8]-,:[#6]   O-C:C-O-C
[#8]-,:c:c-,:[#8;!H0]   O-C:C-O-[#1]
[#6]-,:[#6]-,:[#8]-,:c:c   C-C-O-C:C
[#7]-,:[#6]-,:c:c-,:[#6]   N-C-C:C-C
[#6]-,:[#6]-,:c:c-,:[#6]   C-C-C:C-C
[#7]-,:[#7]-,:[#6]-,:[#7;!H0]   N-N-C-N-[#1]
[#6]-,:[#7]-,:[#6]-,:[#7]-,:[#6]   C-N-C-N-C
[#8]-,:[#6]-,:[#6]-,:[#6]-,:[#6]   O-C-C-C-C
[#8]-,:[#6]-,:[#6]-,:[#6]-,:[#7]   O-C-C-C-N
[#8]-,:[#6]-,:[#6]-,:[#6]-,:[#8]   O-C-C-C-O
[#6]=,:[#6]-,:[#6]-,:[#6]-,:[#6]   C=C-C-C-C
[#8]-,:[#6]-,:[#6]-,:[#6]=,:[#6]   O-C-C-C=C
[#8]-,:[#6]-,:[#6]-,:[#6]=,:[#8]   O-C-C-C=O
[#6;!H0]-,:[#6]-,:[#7;!H0]   [#1]-C-C-N-[#1]
[#6]-,:[#6]=,:[#7]-,:[#7]-,:[#6]   C-C=N-N-C
[#8]=,:[#6]-,:[#7]-,:[#6]-,:[#6]   O=C-N-C-C
[#8]=,:[#6]-,:[#7]-,:[#6;!H0]   O=C-N-C-[#1]
[#8]=,:[#6]-,:[#7]-,:[#6]-,:[#7]   O=C-N-C-N
[#8]=,:[#7]-,:c:c-,:[#7]   O=N-C:C-N
[#8]=,:[#7]-,:c:c-,:[#8]   O=N-C:C-O
[#8]=,:[#6]-,:[#7]-,:[#6]=,:[#8]   O=C-N-C=O
[#8]-,:c:c:c-,:[#6]   O-C:C:C-C
[#8]-,:c:c:c-,:[#7]   O-C:C:C-N
[#8]-,:c:c:c-,:[#8]   O-C:C:C-O
[#7]-,:[#6]-,:[#7]-,:[#6]-,:[#6]   N-C-N-C-C
[#8]-,:[#6]-,:[#6]-,:c:c   O-C-C-C:C
[#6]-,:[#6]-,:[#7]-,:[#6]-,:[#6]   C-C-N-C-C
[#6]-,:[#7]-,:c:c-,:[#6]   C-N-C:C-C
[#6]-,:[#6]-,:[#16]-,:[#6]-,:[#6]   C-C-S-C-C
[#8]-,:[#6]-,:[#6]-,:[#7]-,:[#6]   O-C-C-N-C
[#6]-,:[#6]=,:[#6]-,:[#6]-,:[#6]   C-C=C-C-C
[#8]-,:[#6]-,:[#8]-,:[#6]-,:[#6]   O-C-O-C-C
[#8]-,:[#6]-,:[#6]-,:[#8]-,:[#6]   O-C-C-O-C
[#8]-,:[#6]-,:[#6]-,:[#8;!H0]   O-C-C-O-[#1]
[#6]-,:[#6]=,:[#6]-,:[#6]=,:[#6]   C-C=C-C=C
[#7]-,:c:c-,:[#6]-,:[#6]   N-C:C-C-C
[#6]=,:[#6]-,:[#6]-,:[#8]-,:[#6]   C=C-C-O-C
[#6]=,:[#6]-,:[#6]-,:[#8;!H0]   C=C-C-O-[#1]
[#6]-,:c:c-,:[#6]-,:[#6]   C-C:C-C-C
Cl-,:c:c-,:[#6]=,:[#8]   Cl-C:C-C=O
Br-,:c:c:c-,:[#6]   Br-C:C:C-C
[#8]=,:[#6]-,:[#6]=,:[#6]-,:[#6]   O=C-C=C-C
[#8]=,:[#6]-,:[#6]=,:[#6;!H0]   O=C-C=C-[#1]
[#8]=,:[#6]-,:[#6]=,:[#6]-,:[#7]   O=C-C=C-N
[#7]-,:[#6]-,:[#7]-,:c:c   N-C-N-C:C
Br-,:[#6]-,:[#6]-,:c:c   Br-C-C-C:C
N#[#6]-,:[#6]-,:[#6]-,:[#6]   N#C-C-C-C
[#6]-,:[#6]=,:[#6]-,:c:c   C-C=C-C:C
[#6]-,:[#6]-,:[#6]=,:[#6]-,:[#6]   C-C-C=C-C
[#6]-,:[#6]-,:[#6]-,:[#6]-,:[#6]-,:[#6]   C-C-C-C-C-C
[#8]-,:[#6]-,:[#6]-,:[#6]-,:[#6]-,:[#6]   O-C-C-C-C-C
[#8]-,:[#6]-,:[#6]-,:[#6]-,:[#6]-,:[#8]   O-C-C-C-C-O
[#8]-,:[#6]-,:[#6]-,:[#6]-,:[#6]-,:[#7]   O-C-C-C-C-N
[#7]-,:[#6]-,:[#6]-,:[#6]-,:[#6]-,:[#6]   N-C-C-C-C-C
[#8]=,:[#6]-,:[#6]-,:[#6]-,:[#6]-,:[#6]   O=C-C-C-C-C
[#8]=,:[#6]-,:[#6]-,:[#6]-,:[#6]-,:[#7]   O=C-C-C-C-N
[#8]=,:[#6]-,:[#6]-,:[#6]-,:[#6]-,:[#8]   O=C-C-C-C-O
[#8]=,:[#6]-,:[#6]-,:[#6]-,:[#6]=,:[#8]   O=C-C-C-C=O
[#6]-,:[#6]-,:[#6]-,:[#6]-,:[#6]-,:[#6]-,:[#6]   C-C-C-C-C-C-C
[#8]-,:[#6]-,:[#6]-,:[#6]-,:[#6]-,:[#6]-,:[#6]   O-C-C-C-C-C-C
[#8]-,:[#6]-,:[#6]-,:[#6]-,:[#6]-,:[#6]-,:[#8]   O-C-C-C-C-C-O
[#8]-,:[#6]-,:[#6]-,:[#6]-,:[#6]-,:[#6]-,:[#7]   O-C-C-C-C-C-N
[#8]=,:[#6]-,:[#6]-,:[#6]-,:[#6]-,:[#6]-,:[#6]   O=C-C-C-C-C-C
[#8]=,:[#6]-,:[#6]-,:[#6]-,:[#6]-,:[#6]-,:[#8]   O=C-C-C-C-C-O
[#8]=,:[#6]-,:[#6]-,:[#6]-,:[#6]-,:[#6]=,:[#8]   O=C-C-C-C-C=O
[#8]=,:[#6]-,:[#6]-,:[#6]-,:[#6]-,:[#6]-,:[#7]   O=C-C-C-C-C-N
[#6]-,:[#6]-,:[#6]-,:[#6]-,:[#6]-,:[#6]-,:[#6]-,:[#6]   C-C-C-C-C-C-C-C
[#6]-,:[#6]-,:[#6]-,:[#6]-,:[#6]-,:[#6](-,:[#6])-,:[#6]   C-C-C-C-C-C(C)-C
[#8]-,:[#6]-,:[#6]-,:[#6]-,:[#6]-,:[#6]-,:[#6]-,:[#6]   O-C-C-C-C-C-C-C
[#8]-,:[#6]-,:[#6]-,:[#6]-,:[#6]-,:[#6](-,:[#6])-,:[#6]   O-C-C-C-C-C(C)-C
[#8]-,:[#6]-,:[#6]-,:[#6]-,:[#6]-,:[#6]-,:[#8]-,:[#6]   O-C-C-C-C-C-O-C
[#8]-,:[#6]-,:[#6]-,:[#6]-,:[#6]-,:[#6](-,:[#8])-,:[#6]   O-C-C-C-C-C(O)-C
[#8]-,:[#6]-,:[#6]-,:[#6]-,:[#6]-,:[#6]-,:[#7]-,:[#6]   O-C-C-C-C-C-N-C
[#8]-,:[#6]-,:[#6]-,:[#6]-,:[#6]-,:[#6](-,:[#7])-,:[#6]   O-C-C-C-C-C(N)-C
[#8]=,:[#6]-,:[#6]-,:[#6]-,:[#6]-,:[#6]-,:[#6]-,:[#6]   O=C-C-C-C-C-C-C
[#8]=,:[#6]-,:[#6]-,:[#6]-,:[#6]-,:[#6](-,:[#8])-,:[#6]   O=C-C-C-C-C(O)-C
[#8]=,:[#6]-,:[#6]-,:[#6]-,:[#6]-,:[#6](=,:[#8])-,:[#6]   O=C-C-C-C-C(=O)-C
[#8]=,:[#6]-,:[#6]-,:[#6]-,:[#6]-,:[#6](-,:[#7])-,:[#6]   O=C-C-C-C-C(N)-C
[#6]-,:[#6](-,:[#6])-,:[#6]-,:[#6]   C-C(C)-C-C
[#6]-,:[#6](-,:[#6])-,:[#6]-,:[#6]-,:[#6]   C-C(C)-C-C-C
[#6]-,:[#6]-,:[#6](-,:[#6])-,:[#6]-,:[#6]   C-C-C(C)-C-C
[#6]-,:[#6](-,:[#6])(-,:[#6])-,:[#6]-,:[#6]   C-C(C)(C)-C-C
[#6]-,:[#6](-,:[#6])-,:[#6](-,:[#6])-,:[#6]       C-C(C)-C(C)-C

# Section 7: Complex SMARTS patterns - These bits test for the
# presence of complex SMARTS patterns, regardless of count, but where
# bond orders and bond aromaticity are specific.

[#6]c1ccc([#6])cc1   Cc1ccc(C)cc1
[#6]c1ccc([#8])cc1   Cc1ccc(O)cc1
[#6]c1ccc([#16])cc1   Cc1ccc(S)cc1
[#6]c1ccc([#7])cc1   Cc1ccc(N)cc1
[#6]c1ccc(Cl)cc1   Cc1ccc(Cl)cc1
[#6]c1ccc(Br)cc1   Cc1ccc(Br)cc1
[#8]c1ccc([#8])cc1   Oc1ccc(O)cc1
[#8]c1ccc([#16])cc1   Oc1ccc(S)cc1
[#8]c1ccc([#7])cc1   Oc1ccc(N)cc1
[#8]c1ccc(Cl)cc1   Oc1ccc(Cl)cc1
[#8]c1ccc(Br)cc1   Oc1ccc(Br)cc1
[#16]c1ccc([#16])cc1   Sc1ccc(S)cc1
[#16]c1ccc([#7])cc1   Sc1ccc(N)cc1
[#16]c1ccc(Cl)cc1   Sc1ccc(Cl)cc1
[#16]c1ccc(Br)cc1   Sc1ccc(Br)cc1
[#7]c1ccc([#7])cc1   Nc1ccc(N)cc1
[#7]c1ccc(Cl)cc1   Nc1ccc(Cl)cc1
[#7]c1ccc(Br)cc1   Nc1ccc(Br)cc1
Clc1ccc(Cl)cc1   Clc1ccc(Cl)cc1
Clc1ccc(Br)cc1   Clc1ccc(Br)cc1
Brc1ccc(Br)cc1   Brc1ccc(Br)cc1
[#6]c1cc([#6])ccc1   Cc1cc(C)ccc1
[#6]c1cc([#8])ccc1   Cc1cc(O)ccc1
[#6]c1cc([#16])ccc1   Cc1cc(S)ccc1
[#6]c1cc([#7])ccc1   Cc1cc(N)ccc1
[#6]c1cc(Cl)ccc1   Cc1cc(Cl)ccc1
[#6]c1cc(Br)ccc1   Cc1cc(Br)ccc1
[#8]c1cc([#8])ccc1   Oc1cc(O)ccc1
[#8]c1cc([#16])ccc1   Oc1cc(S)ccc1
[#8]c1cc([#7])ccc1   Oc1cc(N)ccc1
[#8]c1cc(Cl)ccc1   Oc1cc(Cl)ccc1
[#8]c1cc(Br)ccc1   Oc1cc(Br)ccc1
[#16]c1cc([#16])ccc1   Sc1cc(S)ccc1
[#16]c1cc([#7])ccc1   Sc1cc(N)ccc1
[#16]c1cc(Cl)ccc1   Sc1cc(Cl)ccc1
[#16]c1cc(Br)ccc1   Sc1cc(Br)ccc1
[#7]c1cc([#7])ccc1   Nc1cc(N)ccc1
[#7]c1cc(Cl)ccc1   Nc1cc(Cl)ccc1
[#7]c1cc(Br)ccc1   Nc1cc(Br)ccc1
Clc1cc(Cl)ccc1   Clc1cc(Cl)ccc1
Clc1cc(Br)ccc1   Clc1cc(Br)ccc1
Brc1cc(Br)ccc1   Brc1cc(Br)ccc1
[#6]c1c([#6])cccc1   Cc1c(C)cccc1
[#6]c1c([#8])cccc1   Cc1c(O)cccc1
[#6]c1c([#16])cccc1   Cc1c(S)cccc1
[#6]c1c([#7])cccc1   Cc1c(N)cccc1
[#6]c1c(Cl)cccc1   Cc1c(Cl)cccc1
[#6]c1c(Br)cccc1   Cc1c(Br)cccc1
[#8]c1c([#8])cccc1   Oc1c(O)cccc1
[#8]c1c([#16])cccc1   Oc1c(S)cccc1
[#8]c1c([#7])cccc1   Oc1c(N)cccc1
[#8]c1c(Cl)cccc1   Oc1c(Cl)cccc1
[#8]c1c(Br)cccc1   Oc1c(Br)cccc1
[#16]c1c([#16])cccc1   Sc1c(S)cccc1
[#16]c1c([#7])cccc1   Sc1c(N)cccc1
[#16]c1c(Cl)cccc1   Sc1c(Cl)cccc1
[#16]c1c(Br)cccc1   Sc1c(Br)cccc1
[#7]c1c([#7])cccc1   Nc1c(N)cccc1
[#7]c1c(Cl)cccc1   Nc1c(Cl)cccc1
[#7]c1c(Br)cccc1   Nc1c(Br)cccc1
Clc1c(Cl)cccc1   Clc1c(Cl)cccc1
Clc1c(Br)cccc1   Clc1c(Br)cccc1
Brc1c(Br)cccc1   Brc1c(Br)cccc1
[#6][#6]1[#6][#6][#6]([#6])[#6][#6]1   CC1CCC(C)CC1
[#6][#6]1[#6][#6][#6]([#8])[#6][#6]1   CC1CCC(O)CC1
[#6][#6]1[#6][#6][#6]([#16])[#6][#6]1   CC1CCC(S)CC1
[#6][#6]1[#6][#6][#6]([#7])[#6][#6]1   CC1CCC(N)CC1
[#6][#6]1[#6][#6][#6](Cl)[#6][#6]1   CC1CCC(Cl)CC1
[#6][#6]1[#6][#6][#6](Br)[#6][#6]1   CC1CCC(Br)CC1
[#8][#6]1[#6][#6][#6]([#8])[#6][#6]1   OC1CCC(O)CC1
[#8][#6]1[#6][#6][#6]([#16])[#6][#6]1   OC1CCC(S)CC1
[#8][#6]1[#6][#6][#6]([#7])[#6][#6]1   OC1CCC(N)CC1
[#8][#6]1[#6][#6][#6](Cl)[#6][#6]1   OC1CCC(Cl)CC1
[#8][#6]1[#6][#6][#6](Br)[#6][#6]1   OC1CCC(Br)CC1
[#16][#6]1[#6][#6][#6]([#16])[#6][#6]1   SC1CCC(S)CC1
[#16][#6]1[#6][#6][#6]([#7])[#6][#6]1   SC1CCC(N)CC1
[#16][#6]1[#6][#6][#6](Cl)[#6][#6]1   SC1CCC(Cl)CC1
[#16][#6]1[#6][#6][#6](Br)[#6][#6]1   SC1CCC(Br)CC1
[#7][#6]1[#6][#6][#6]([#7])[#6][#6]1   NC1CCC(N)CC1
[#7][#6]1[#6][#6][#6](Cl)[#6][#6]1   NC1CCC(Cl)CC1
[#7][#6]1[#6][#6][#6](Br)[#6][#6]1   NC1CCC(Br)CC1
Cl[#6]1[#6][#6][#6](Cl)[#6][#6]1   ClC1CCC(Cl)CC1
Cl[#6]1[#6][#6][#6](Br)[#6][#6]1   ClC1CCC(Br)CC1
Br[#6]1[#6][#6][#6](Br)[#6][#6]1   BrC1CCC(Br)CC1
[#6][#6]1[#6][#6]([#6])[#6][#6][#6]1   CC1CC(C)CCC1
[#6][#6]1[#6][#6]([#8])[#6][#6][#6]1   CC1CC(O)CCC1
[#6][#6]1[#6][#6]([#16])[#6][#6][#6]1   CC1CC(S)CCC1
[#6][#6]1[#6][#6]([#7])[#6][#6][#6]1   CC1CC(N)CCC1
[#6][#6]1[#6][#6](Cl)[#6][#6][#6]1   CC1CC(Cl)CCC1
[#6][#6]1[#6][#6](Br)[#6][#6][#6]1   CC1CC(Br)CCC1
[#8][#6]1[#6][#6]([#8])[#6][#6][#6]1   OC1CC(O)CCC1
[#8][#6]1[#6][#6]([#16])[#6][#6][#6]1   OC1CC(S)CCC1
[#8][#6]1[#6][#6]([#7])[#6][#6][#6]1   OC1CC(N)CCC1
[#8][#6]1[#6][#6](Cl)[#6][#6][#6]1   OC1CC(Cl)CCC1
[#8][#6]1[#6][#6](Br)[#6][#6][#6]1   OC1CC(Br)CCC1
[#16][#6]1[#6][#6]([#16])[#6][#6][#6]1   SC1CC(S)CCC1
[#16][#6]1[#6][#6]([#7])[#6][#6][#6]1   SC1CC(N)CCC1
[#16][#6]1[#6][#6](Cl)[#6][#6][#6]1   SC1CC(Cl)CCC1
[#16][#6]1[#6][#6](Br)[#6][#6][#6]1   SC1CC(Br)CCC1
[#7][#6]1[#6][#6]([#7])[#6][#6][#6]1   NC1CC(N)CCC1
[#7][#6]1[#6][#6](Cl)[#6][#6][#6]1   NC1CC(Cl)CCC1
[#7][#6]1[#6][#6](Br)[#6][#6][#6]1   NC1CC(Br)CCC1
Cl[#6]1[#6][#6](Cl)[#6][#6][#6]1   ClC1CC(Cl)CCC1
Cl[#6]1[#6][#6](Br)[#6][#6][#6]1   ClC1CC(Br)CCC1
Br[#6]1[#6][#6](Br)[#6][#6][#6]1   BrC1CC(Br)CCC1
[#6][#6]1[#6]([#6])[#6][#6][#6][#6]1   CC1C(C)CCCC1
[#6][#6]1[#6]([#8])[#6][#6][#6][#6]1   CC1C(O)CCCC1
[#6][#6]1[#6]([#16])[#6][#6][#6][#6]1   CC1C(S)CCCC1
[#6][#6]1[#6]([#7])[#6][#6][#6][#6]1   CC1C(N)CCCC1
[#6][#6]1[#6](Cl)[#6][#6][#6][#6]1   CC1C(Cl)CCCC1
[#6][#6]1[#6](Br)[#6][#6][#6][#6]1   CC1C(Br)CCCC1
[#8][#6]1[#6]([#8])[#6][#6][#6][#6]1   OC1C(O)CCCC1
[#8][#6]1[#6]([#16])[#6][#6][#6][#6]1   OC1C(S)CCCC1
[#8][#6]1[#6]([#7])[#6][#6][#6][#6]1   OC1C(N)CCCC1
[#8][#6]1[#6](Cl)[#6][#6][#6][#6]1   OC1C(Cl)CCCC1
[#8][#6]1[#6](Br)[#6][#6][#6][#6]1   OC1C(Br)CCCC1
[#16][#6]1[#6]([#16])[#6][#6][#6][#6]1   SC1C(S)CCCC1
[#16][#6]1[#6]([#7])[#6][#6][#6][#6]1   SC1C(N)CCCC1
[#16][#6]1[#6](Cl)[#6][#6][#6][#6]1   SC1C(Cl)CCCC1
[#16][#6]1[#6](Br)[#6][#6][#6][#6]1   SC1C(Br)CCCC1
[#7][#6]1[#6]([#7])[#6][#6][#6][#6]1   NC1C(N)CCCC1
[#7][#6]1[#6](Cl)[#6][#6][#6][#6]1   NC1C(Cl)CCCC1
[#7][#6]1[#6](Br)[#6][#6][#6][#6]1   NC1C(Br)CCCC1
Cl[#6]1[#6](Cl)[#6][#6][#6][#6]1   ClC1C(Cl)CCCC1
Cl[#6]1[#6](Br)[#6][#6][#6][#6]1   ClC1C(Br)CCCC1
Br[#6]1[#6](Br)[#6][#6][#6][#6]1   BrC1C(Br)CCCC1
[#6][#6]1[#6][#6]([#6])[#6][#6]1   CC1CC(C)CC1
[#6][#6]1[#6][#6]([#8])[#6][#6]1   CC1CC(O)CC1
[#6][#6]1[#6][#6]([#16])[#6][#6]1   CC1CC(S)CC1
[#6][#6]1[#6][#6]([#7])[#6][#6]1   CC1CC(N)CC1
[#6][#6]1[#6][#6](Cl)[#6][#6]1   CC1CC(Cl)CC1
[#6][#6]1[#6][#6](Br)[#6][#6]1   CC1CC(Br)CC1
[#8][#6]1[#6][#6]([#8])[#6][#6]1   OC1CC(O)CC1
[#8][#6]1[#6][#6]([#16])[#6][#6]1   OC1CC(S)CC1
[#8][#6]1[#6][#6]([#7])[#6][#6]1   OC1CC(N)CC1
[#8][#6]1[#6][#6](Cl)[#6][#6]1   OC1CC(Cl)CC1
[#8][#6]1[#6][#6](Br)[#6][#6]1   OC1CC(Br)CC1
[#16][#6]1[#6][#6]([#16])[#6][#6]1   SC1CC(S)CC1
[#16][#6]1[#6][#6]([#7])[#6][#6]1   SC1CC(N)CC1
[#16][#6]1[#6][#6](Cl)[#6][#6]1   SC1CC(Cl)CC1
[#16][#6]1[#6][#6](Br)[#6][#6]1   SC1CC(Br)CC1
[#7][#6]1[#6][#6]([#7])[#6][#6]1   NC1CC(N)CC1
[#7][#6]1[#6][#6](Cl)[#6][#6]1   NC1CC(Cl)CC1
[#7][#6]1[#6][#6](Br)[#6][#6]1   NC1CC(Br)CC1
Cl[#6]1[#6][#6](Cl)[#6][#6]1   ClC1CC(Cl)CC1
Cl[#6]1[#6][#6](Br)[#6][#6]1   ClC1CC(Br)CC1
Br[#6]1[#6][#6](Br)[#6][#6]1   BrC1CC(Br)CC1
[#6][#6]1[#6]([#6])[#6][#6][#6]1   CC1C(C)CCC1
[#6][#6]1[#6]([#8])[#6][#6][#6]1   CC1C(O)CCC1
[#6][#6]1[#6]([#16])[#6][#6][#6]1   CC1C(S)CCC1
[#6][#6]1[#6]([#7])[#6][#6][#6]1   CC1C(N)CCC1
[#6][#6]1[#6](Cl)[#6][#6][#6]1   CC1C(Cl)CCC1
[#6][#6]1[#6](Br)[#6][#6][#6]1   CC1C(Br)CCC1
[#8][#6]1[#6]([#8])[#6][#6][#6]1   OC1C(O)CCC1
[#8][#6]1[#6]([#16])[#6][#6][#6]1   OC1C(S)CCC1
[#8][#6]1[#6]([#7])[#6][#6][#6]1   OC1C(N)CCC1
[#8][#6]1[#6](Cl)[#6][#6][#6]1   OC1C(Cl)CCC1
[#8][#6]1[#6](Br)[#6][#6][#6]1   OC1C(Br)CCC1
[#16][#6]1[#6]([#16])[#6][#6][#6]1   SC1C(S)CCC1
[#16][#6]1[#6]([#7])[#6][#6][#6]1   SC1C(N)CCC1
[#16][#6]1[#6](Cl)[#6][#6][#6]1   SC1C(Cl)CCC1
[#16][#6]1[#6](Br)[#6][#6][#6]1   SC1C(Br)CCC1
[#7][#6]1[#6]([#7])[#6][#6][#6]1   NC1C(N)CCC1
[#7][#6]1[#6](Cl)[#6][#6]1   NC1C(Cl)CC1
[#7][#6]1[#6](Br)[#6][#6][#6]1   NC1C(Br)CCC1
Cl[#6]1[#6](Cl)[#6][#6][#6]1   ClC1C(Cl)CCC1
Cl[#6]1[#6](Br)[#6][#6][#6]1   ClC1C(Br)CCC1
Br[#6]1[#6](Br)[#6][#6][#6]1   BrC1C(Br)CCC1
