import glob, re
t = e = f = 0
for p in glob.glob('target/surefire-reports/*.xml'):
    s = open(p).read()
    mt = re.search(r'tests="([0-9]+)"', s)
    mf = re.search(r'failures="([0-9]+)"', s)
    me = re.search(r'errors="([0-9]+)"', s)
    t += int(mt.group(1)) if mt else 0
    f += int(mf.group(1)) if mf else 0
    e += int(me.group(1)) if me else 0
print(f'Test counts: {t} pruebas, {f} fallos, {e} errores')
