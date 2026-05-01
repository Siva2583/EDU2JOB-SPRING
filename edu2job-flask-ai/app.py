from flask import Flask, request, jsonify
import joblib
import pandas as pd
import numpy as np

app = Flask(__name__)

loaded_pipeline = joblib.load('ml_models/job_role_model.pkl')
label_encoder = joblib.load('ml_models/label_encoder.pkl')
mlb_encoder = joblib.load('ml_models/mlb_encoder.pkl')

JOB_SCOPES = {
    "Software Engineer": "Software Engineers design, develop, and maintain software systems that power modern technology across industries. Their analytical skills and problem-solving capabilities make them vital in driving innovation and ensuring product reliability.",
    "Embedded Systems Engineer": "Embedded Systems Engineers create intelligent devices embedded within electronics for IoT, automotive, and robotics sectors. They fuse hardware and software expertise to deliver efficient, real-time embedded solutions.",
    "Financial Analyst": "Financial Analysts interpret complex data, create financial models, and help organizations make strategic decisions. They play a critical role in investment planning, budgeting, and ensuring fiscal health.",
    "Mechanical Engineer": "Mechanical Engineers design and innovate mechanical systems for industries like automotive, robotics, and manufacturing. Their deep knowledge in physics and materials science transforms ideas into practical solutions.",
    "Investment Banker": "Investment Bankers advise on financial strategies, manage mergers and acquisitions, and raise capital for businesses. This dynamic field offers fast-paced challenges and substantial rewards for high performers.",
    "Design Engineer": "Design Engineers use creative and technical expertise to bring new products and solutions to life. They work with multidisciplinary teams to solve complex design problems and drive competitiveness.",
    "Data Scientist": "Data Scientists turn raw data into actionable insights using statistics, machine learning, and visualization tools. Their analysis informs high-level decisions and helps organizations stay ahead of the competition.",
    "Accountant": "Accountants specialize in preparing, examining, and maintaining financial records to ensure accuracy and compliance. They help organizations manage budgets, taxes, and audits while supporting strategic financial goals.",
    "Backend Developer": "Backend Developers build and maintain the server-side logic and databases that power modern applications. They ensure software systems are robust, scalable, and perform efficiently behind the scenes.",
    "Business Analyst": "Business Analysts bridge the gap between business needs and technical solutions by analyzing processes and data. They recommend strategies that drive business improvement, optimize performance, and facilitate informed decisions.",
    "Data Analyst": "Data Analysts process and interpret large datasets to extract trends, patterns, and actionable insights. They support organizational decision-making using statistical tools and data visualization.",
    "Economist": "Economists study the production and distribution of resources, goods, and services by analyzing data and economic trends. They provide insights that influence business strategies and public policies.",
    "Electrical Engineer": "Electrical Engineers design, develop, and test electrical systems and components for a range of industries. Their expertise spans power generation, circuitry, and electronics from conception to implementation.",
    "Electronics Engineer": "Electronics Engineers focus on designing and developing electronic equipment such as circuits, devices, and communications systems. They play a pivotal role in advancing consumer electronics, telecommunications, and digital technology.",
    "Manufacturing Engineer": "Manufacturing Engineers optimize manufacturing processes to increase efficiency, quality, and safety in production lines. They apply technical skills to troubleshoot issues, improve systems, and ensure smooth operations."
}

@app.route('/predict', methods=['POST'])
def predict():
    try:
        data = request.get_json()
        
        cgpa_val = data.get("cgpa")
        if not cgpa_val:
            cgpa_val = 0.0
        else:
            cgpa_val = float(cgpa_val)
            
        raw_input_data = {
            "degree": data.get("degree", ""),
            "major": data.get("major", ""),
            "cgpa": cgpa_val,
        }
        
        skills_raw = data.get("skills", "")
        if not skills_raw:
            skills_list = []
        else:
            skills_list = [skill.strip().upper() for skill in skills_raw.split(',')]
            
        encoded_skills = mlb_encoder.transform([skills_list])
        skills_df = pd.DataFrame(encoded_skills, columns=mlb_encoder.classes_)
        
        input_df = pd.DataFrame([raw_input_data])
        final_df = pd.concat([input_df.reset_index(drop=True), skills_df.reset_index(drop=True)], axis=1)
        
        probabilities = loaded_pipeline.predict_proba(final_df)[0]
        class_names = label_encoder.classes_
        top_5_indices = np.argsort(probabilities)[::-1][:5]
        
        predictions = []
        threshold = 0.04
        
        for i in top_5_indices:
            role = class_names[i]
            score = probabilities[i]
            if score >= threshold:
                predictions.append({
                    'role': role,
                    'score': f"{score * 100:.1f}",
                    'scope': JOB_SCOPES.get(role, 'A promising field with opportunities.')
                })
                
        return jsonify({"predictions": predictions})
        
    except Exception as e:
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    app.run(port=5000)