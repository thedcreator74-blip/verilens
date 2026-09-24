"""Category Classification Engine.

Classifies claims into one of the 13 canonical verification domains
using weighted keyword dictionaries, contextual entity patterns, and score normalization.
"""

import re
from typing import Tuple, Dict, List

# Category keyword dictionaries with relevance weights
CATEGORY_TAXONOMY: Dict[str, Dict[str, float]] = {
    "Government": {
        "government": 3.0, "ministry": 3.5, "pib": 4.0, "scheme": 3.0, "yojna": 3.5,
        "yojana": 3.5, "subsidy": 3.0, "notification": 2.5, "cabinet": 3.0, "parliament": 3.0,
        "aadhaar": 3.5, "pan card": 3.0, "ration": 3.0, "welfare": 2.5, "gazette": 3.5,
        "official order": 3.5, "pension": 2.5, "scholarship": 2.5, "municipal": 2.0,
        "modi": 2.0, "prime minister": 3.0, "president": 2.5, "chief minister": 2.5
    },
    "Politics": {
        "election": 4.0, "bjp": 3.5, "congress": 3.5, "aap": 3.0, "party": 2.0,
        "vote": 3.0, "voters": 3.0, "campaign": 2.5, "opposition": 2.5, "rally": 2.5,
        "manifesto": 3.5, "mp": 2.0, "mla": 2.0, "democracy": 2.0, "senate": 3.0,
        "democrat": 3.0, "republican": 3.0, "poll": 2.5, "coalition": 2.5
    },
    "Health": {
        "who": 3.5, "cdc": 3.5, "nih": 3.5, "icmr": 4.0, "vaccine": 4.0,
        "virus": 3.5, "covid": 3.5, "cancer": 3.0, "doctor": 2.5, "hospital": 2.5,
        "medicine": 3.0, "drug": 2.5, "disease": 3.0, "fda": 3.5, "clinical trial": 3.5,
        "symptoms": 3.0, "infection": 3.0, "pandemic": 3.5, "health": 2.5, "cure": 3.0
    },
    "Technology": {
        "ai": 3.0, "artificial intelligence": 3.5, "google": 2.5, "apple": 2.5, "microsoft": 2.5,
        "openai": 3.5, "gemini": 3.5, "chatgpt": 3.5, "smartphone": 2.5, "android": 3.0,
        "ios": 3.0, "software": 2.5, "cyber": 3.0, "malware": 3.5, "hacker": 3.0,
        "data leak": 3.5, "quantum": 3.0, "chip": 2.5, "semiconductor": 3.0, "gpu": 3.0
    },
    "Finance": {
        "rbi": 4.0, "sebi": 4.0, "bank": 3.0, "interest rate": 3.5, "inflation": 3.0,
        "gdp": 3.0, "stock": 3.0, "sensex": 3.5, "nifty": 3.5, "cryptocurrency": 3.5,
        "bitcoin": 3.5, "tax": 3.0, "income tax": 3.5, "gst": 3.5, "loan": 2.5,
        "rupee": 2.5, "dollar": 2.5, "forex": 3.0, "mutual fund": 3.0, "ipo": 3.5
    },
    "Education": {
        "ugc": 4.0, "aicte": 4.0, "cbse": 4.0, "exam": 3.0, "university": 3.0,
        "school": 2.5, "neet": 4.0, "jee": 4.0, "syllabus": 3.0, "result": 2.5,
        "admissions": 2.5, "degree": 2.5, "board exam": 3.5, "students": 2.0,
        "college": 2.5, "curriculum": 3.0
    },
    "Business": {
        "merger": 3.5, "acquisition": 3.5, "ceo": 3.0, "quarterly earnings": 3.5,
        "revenue": 3.0, "tata": 2.5, "reliance": 2.5, "adani": 2.5, "layoffs": 3.0,
        "startup": 2.5, "valuation": 3.0, "e-commerce": 2.5, "amazon": 2.0, "tesla": 2.5
    },
    "Science": {
        "isro": 4.0, "nasa": 4.0, "space": 3.0, "moon": 2.5, "mars": 3.0,
        "satellite": 3.0, "astronomy": 3.5, "physics": 3.0, "telescope": 3.0,
        "rover": 3.0, "james webb": 4.0, "chandrayaan": 4.0, "black hole": 3.5
    },
    "Sports": {
        "cricket": 3.5, "bcci": 4.0, "icc": 4.0, "ipl": 3.5, "world cup": 3.5,
        "football": 3.0, "fifa": 4.0, "olympics": 4.0, "medal": 3.0, "match": 2.0,
        "tennis": 3.0, "badminton": 3.0, "goal": 2.0, "tournament": 2.5, "athlete": 2.5
    },
    "Weather": {
        "cyclone": 4.0, "earthquake": 4.0, "imd": 4.0, "tsunami": 4.0, "flood": 3.5,
        "rainfall": 3.0, "monsoon": 3.5, "heatwave": 3.5, "storm": 3.0, "temperature": 2.5,
        "warning": 2.0, "red alert": 3.5, "weather forecast": 3.5
    },
    "Crime": {
        "police": 3.0, "arrest": 3.5, "cbi": 4.0, "ed": 4.0, "scam": 3.5,
        "fraud": 3.5, "murder": 3.5, "court": 2.5, "judge": 2.5, "bail": 3.0,
        "smuggling": 3.5, "bribe": 3.5, "investigation": 2.5, "cybercrime": 3.5
    },
    "Entertainment": {
        "movie": 3.0, "actor": 3.0, "actress": 3.0, "bollywood": 3.5, "hollywood": 3.5,
        "box office": 3.5, "trailer": 3.0, "film": 2.5, "director": 2.5, "song": 2.0,
        "celebrity": 2.5, "netflix": 2.5, "series": 2.0, "ott": 3.0
    }
}


class CategoryClassifier:
    """Classifies claim texts into canonical topical categories."""

    @classmethod
    def classify(cls, text: str) -> Tuple[str, float]:
        """
        Calculates category match scores and returns (category_name, confidence).
        Falls back to 'General' if score is below threshold.
        """
        if not text or not text.strip():
            return "General", 50.0

        lowered = f" {text.lower()} "
        scores: Dict[str, float] = {cat: 0.0 for cat in CATEGORY_TAXONOMY}

        for category, keywords in CATEGORY_TAXONOMY.items():
            for keyword, weight in keywords.items():
                # Word boundary match
                pattern = r"\b" + re.escape(keyword) + r"\b"
                matches = len(re.findall(pattern, lowered))
                if matches > 0:
                    scores[category] += weight * matches

        # Find best category
        sorted_scores = sorted(scores.items(), key=lambda x: x[1], reverse=True)
        best_category, best_score = sorted_scores[0]

        if best_score < 2.0:
            return "General", 60.0

        # Calculate confidence percentage (bounded 65 to 98)
        second_score = sorted_scores[1][1] if len(sorted_scores) > 1 else 0.0
        margin = best_score - second_score
        
        confidence = min(65.0 + (best_score * 5.0) + (margin * 3.0), 98.0)
        return best_category, round(confidence, 1)
