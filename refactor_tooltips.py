import os
import re

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    original_content = content
    
    # 1. Add import for TooltipIconButton if not present
    if 'import com.project.ui.components.TooltipIconButton' not in content:
        import_stmt = 'import com.project.ui.components.TooltipIconButton\n'
        content = re.sub(r'(import androidx.[^\n]+)(?!.*import androidx)', r'\1\n' + import_stmt, content, count=1, flags=re.DOTALL)

    # We will look for IconButton(args) { Icon(..., contentDescription = "XYZ"...) } 
    # and replace IconButton with TooltipIconButton(tooltipText = "XYZ", args)
    
    # Single pass regex:
    # Match: IconButton( args ) { body_containing_contentDescription }
    # We must ensure we don't match greedy across multiple IconButtons.
    # Group 1: args
    # Group 2: body
    # Group 3: tooltip text inside body
    
    pattern = r'IconButton\s*\(\s*(.*?)\s*\)\s*\{([^}]*?contentDescription\s*=\s*"([^"]+)"[^}]*)\}'
    
    def replacer(match):
        args = match.group(1)
        body = match.group(2)
        tooltip = match.group(3)
        
        # Don't add tooltip if it's empty, null or already a TooltipIconButton
        if tooltip == "null" or not tooltip:
            return match.group(0)
            
        if args:
            return f'TooltipIconButton(tooltipText = "{tooltip}", {args}) {{{body}}}'
        else:
            return f'TooltipIconButton(tooltipText = "{tooltip}") {{{body}}}'

    new_content = re.sub(pattern, replacer, content)
    
    if new_content != original_content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"Refactored: {filepath}")

def main():
    search_dir = r"C:\Users\ferna\StudioProjects\TCC2-Luis\app\src\main\java\com\project\ui"
    for root, dirs, files in os.walk(search_dir):
        for file in files:
            if file.endswith(".kt") and file != "TooltipIconButton.kt":
                process_file(os.path.join(root, file))

if __name__ == "__main__":
    main()
