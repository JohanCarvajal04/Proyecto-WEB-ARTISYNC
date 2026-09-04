import csv
import os
from collections import defaultdict

file_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'target', 'site', 'jacoco', 'jacoco.csv')
try:
    with open(file_path, 'r', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        
        stats_by_package = defaultdict(lambda: {
            'line_missed': 0, 'line_covered': 0, 'branch_missed': 0, 'branch_covered': 0
        })
        
        classes = []
        
        for row in reader:
            pkg = row['PACKAGE']
            cls = row['CLASS']
            lm = int(row['LINE_MISSED'])
            lc = int(row['LINE_COVERED'])
            bm = int(row['BRANCH_MISSED'])
            bc = int(row['BRANCH_COVERED'])
            
            stats_by_package[pkg]['line_missed'] += lm
            stats_by_package[pkg]['line_covered'] += lc
            stats_by_package[pkg]['branch_missed'] += bm
            stats_by_package[pkg]['branch_covered'] += bc
            
            classes.append({
                'pkg': pkg, 'cls': cls,
                'lm': lm, 'lc': lc, 'bm': bm, 'bc': bc
            })
            
        def print_stats(name, stats):
            lc = stats['line_covered']
            lt = lc + stats['line_missed']
            bc = stats['branch_covered']
            bt = bc + stats['branch_missed']
            
            lp = (lc / lt * 100) if lt > 0 else 100
            bp = (bc / bt * 100) if bt > 0 else 100
            
            print(f'{name}: Lines: {lc}/{lt} ({lp:.2f}%) | Branches: {bc}/{bt} ({bp:.2f}%)')
            
        global_stats = {
            'line_missed': 0, 'line_covered': 0, 'branch_missed': 0, 'branch_covered': 0
        }
        
        service_stats = {
            'line_missed': 0, 'line_covered': 0, 'branch_missed': 0, 'branch_covered': 0
        }
        
        controller_stats = {
            'line_missed': 0, 'line_covered': 0, 'branch_missed': 0, 'branch_covered': 0
        }
        
        for pkg, stats in stats_by_package.items():
            for k in global_stats:
                global_stats[k] += stats[k]
                
            if 'service' in pkg.lower():
                for k in service_stats:
                    service_stats[k] += stats[k]
                    
            if 'controller' in pkg.lower():
                for k in controller_stats:
                    controller_stats[k] += stats[k]
                    
        print_stats('GLOBAL', global_stats)
        print_stats('SERVICIOS', service_stats)
        print_stats('CONTROLADORES', controller_stats)
        
        print('\n--- TOP 10 CONTROLADORES CON PEOR COBERTURA (Lineas faltantes) ---')
        controllers = [c for c in classes if 'controller' in c['pkg'].lower()]
        controllers.sort(key=lambda x: x['lm'], reverse=True)
        for c in controllers[:10]:
            print(f"{c['cls']}: faltan {c['lm']} lineas, cubiertas {c['lc']}")
            
        print('\n--- TOP 10 SERVICIOS CON PEOR COBERTURA (Ramas faltantes) ---')
        services = [c for c in classes if 'service' in c['pkg'].lower()]
        services.sort(key=lambda x: x['bm'], reverse=True)
        for c in services[:10]:
            print(f"{c['cls']}: faltan {c['bm']} ramas, cubiertas {c['bc']}")
except FileNotFoundError:
    print('jacoco.csv not found.')
