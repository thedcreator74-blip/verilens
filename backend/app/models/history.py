"""SQLAlchemy model for verification history and evidence records."""

import datetime
from sqlalchemy import Column, Integer, String, Text, Float, DateTime, JSON
from app.database.session import Base


class VerificationHistory(Base):
    __tablename__ = "verification_history"

    id = Column(Integer, primary_key=True, index=True, autoincrement=True)
    input_type = Column(String(32), nullable=False, index=True)  # IMAGE, TEXT, LINK
    raw_input = Column(Text, nullable=False)
    extracted_claim = Column(Text, nullable=False)
    normalized_claim = Column(Text, nullable=False, index=True)
    category = Column(String(64), nullable=False, index=True)
    category_confidence = Column(Float, nullable=False, default=0.0)
    verification_strategy = Column(String(128), nullable=False)
    
    # JSON columns for rich evidence & source packages
    sources = Column(JSON, nullable=False, default=list)
    evidence = Column(JSON, nullable=False, default=list)
    metrics = Column(JSON, nullable=False, default=dict)

    created_at = Column(DateTime, default=datetime.datetime.utcnow, nullable=False, index=True)

    def to_dict(self):
        return {
            "id": self.id,
            "input_type": self.input_type,
            "raw_input": self.raw_input,
            "claim": self.extracted_claim,
            "normalized_claim": self.normalized_claim,
            "category": self.category,
            "confidence": self.category_confidence,
            "verification_strategy": self.verification_strategy,
            "sources": self.sources,
            "evidence": self.evidence,
            "metrics": self.metrics,
            "created_at": self.created_at.isoformat() if self.created_at else None
        }
