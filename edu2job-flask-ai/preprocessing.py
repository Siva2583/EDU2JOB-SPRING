import json

def preprocess_input(raw_data):
    processed = {}

    cgpa_value = raw_data.get("cgpa", "")
    if cgpa_value.strip() != "":
        processed["cgpa"] = float(cgpa_value)
    else:
        processed["cgpa"] = None

    yop_value = raw_data.get("year_of_graduation", "")
    if yop_value.strip() != "":
        processed["year_of_graduation"] = int(yop_value)
    else:
        processed["year_of_graduation"] = None

    degree_value = raw_data.get("degree", "")
    if degree_value == "B.Tech":
        processed["degree_encoded"] = 0
    elif degree_value == "M.Tech":
        processed["degree_encoded"] = 1
    elif degree_value == "MBA":
        processed["degree_encoded"] = 2
    elif degree_value == "B.Sc":
        processed["degree_encoded"] = 3
    elif degree_value == "M.Sc":
        processed["degree_encoded"] = 4
    else:
        processed["degree_encoded"] = -1

    processed["major"] = raw_data.get("major", "").lower()

    skills_raw = raw_data.get("skills", "[]")
    try:
        skills_list = json.loads(skills_raw)
    except:
        skills_list = []

    processed["skill_python"] = 1 if "Python" in skills_list else 0
    processed["skill_java"]   = 1 if "Java" in skills_list else 0
    processed["skill_cpp"]    = 1 if "C++" in skills_list else 0
    processed["skill_sql"]    = 1 if "SQL" in skills_list else 0
    processed["skill_django"] = 1 if "Django" in skills_list else 0
    processed["skill_react"]  = 1 if "React" in skills_list else 0

    return processed