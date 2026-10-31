"""
TalentMatch AI - Versioned Skill Taxonomy & Normalization Engine
Taxonomy Version: 1.0.0
"""

from typing import Dict, List, Optional, Set
from dataclasses import dataclass, field


TAXONOMY_VERSION = "1.0.0"


@dataclass(frozen=True)
class CanonicalSkill:
    id: str
    name: str
    category: str
    aliases: List[str] = field(default_factory=list)


# Comprehensive taxonomy of industry standard software engineering competencies
CANONICAL_SKILLS: List[CanonicalSkill] = [
    # Languages
    CanonicalSkill("lang-java", "Java", "Languages", ["java", "core java", "java 21", "java 17", "java 11", "java 8", "jdk"]),
    CanonicalSkill("lang-python", "Python", "Languages", ["python", "python3", "python 3", "py3"]),
    CanonicalSkill("lang-typescript", "TypeScript", "Languages", ["typescript", "type script", "ts"]),
    CanonicalSkill("lang-javascript", "JavaScript", "Languages", ["javascript", "js", "ecmascript", "es6", "es2020", "vanilla js"]),
    CanonicalSkill("lang-cpp", "C++", "Languages", ["c++", "cpp", "c / c++", "modern c++"]),
    CanonicalSkill("lang-csharp", "C#", "Languages", ["c#", "csharp", "c-sharp", ".net c#"]),
    CanonicalSkill("lang-go", "Go", "Languages", ["golang", "go language", "go lang"]),
    CanonicalSkill("lang-rust", "Rust", "Languages", ["rust", "rust-lang"]),
    CanonicalSkill("lang-sql", "SQL", "Languages", ["sql", "structured query language", "ansi sql", "t-sql", "pl/sql"]),

    # Backend Frameworks
    CanonicalSkill("fw-spring-boot", "Spring Boot", "Backend Frameworks", ["spring boot", "springboot", "spring-boot", "spring framework", "spring", "spring mvc", "spring cloud"]),
    CanonicalSkill("fw-hibernate", "Hibernate/JPA", "Backend Frameworks", ["hibernate", "jpa", "spring data jpa", "java persistence api"]),
    CanonicalSkill("fw-fastapi", "FastAPI", "Backend Frameworks", ["fastapi", "fast-api", "fast api"]),
    CanonicalSkill("fw-django", "Django", "Backend Frameworks", ["django", "django rest framework", "drf"]),
    CanonicalSkill("fw-flask", "Flask", "Backend Frameworks", ["flask"]),
    CanonicalSkill("fw-nodejs", "Node.js", "Backend Frameworks", ["node.js", "nodejs", "node js", "node"]),
    CanonicalSkill("fw-express", "Express.js", "Backend Frameworks", ["express", "express.js", "expressjs"]),
    CanonicalSkill("fw-dotnet", ".NET Core", "Backend Frameworks", [".net", ".net core", "asp.net", "asp.net core", "dotnet"]),

    # Frontend Frameworks
    CanonicalSkill("fw-react", "React", "Frontend", ["react", "react.js", "reactjs", "react native"]),
    CanonicalSkill("fw-angular", "Angular", "Frontend", ["angular", "angularjs", "angular 2+"]),
    CanonicalSkill("fw-vue", "Vue.js", "Frontend", ["vue", "vue.js", "vuejs", "vue 3"]),
    CanonicalSkill("fw-nextjs", "Next.js", "Frontend", ["next.js", "nextjs", "next js"]),
    CanonicalSkill("fe-html-css", "HTML5/CSS3", "Frontend", ["html", "html5", "css", "css3", "sass", "scss", "tailwind", "tailwindcss", "bootstrap"]),

    # Databases & Caching
    CanonicalSkill("db-mysql", "MySQL", "Databases", ["mysql", "my-sql", "mysql 8", "aurora mysql"]),
    CanonicalSkill("db-postgres", "PostgreSQL", "Databases", ["postgresql", "postgres", "psql", "postgre sql", "aurora postgres"]),
    CanonicalSkill("db-mongodb", "MongoDB", "Databases", ["mongodb", "mongo", "documentdb"]),
    CanonicalSkill("db-redis", "Redis", "Databases", ["redis", "redis cache", "redis cluster"]),
    CanonicalSkill("db-elasticsearch", "Elasticsearch", "Databases", ["elasticsearch", "elastic search", "opensearch", "elk stack"]),

    # Cloud & DevOps
    CanonicalSkill("cloud-aws", "Amazon Web Services (AWS)", "Cloud & DevOps", ["aws", "amazon web services", "aws cloud", "s3", "ec2", "lambda", "ecs", "eks", "fargate"]),
    CanonicalSkill("cloud-azure", "Microsoft Azure", "Cloud & DevOps", ["azure", "microsoft azure", "azure cloud", "azure devops"]),
    CanonicalSkill("cloud-gcp", "Google Cloud Platform (GCP)", "Cloud & DevOps", ["gcp", "google cloud", "google cloud platform", "bigquery", "gke"]),
    CanonicalSkill("devops-docker", "Docker", "Cloud & DevOps", ["docker", "docker compose", "containerization", "containers"]),
    CanonicalSkill("devops-kubernetes", "Kubernetes", "Cloud & DevOps", ["kubernetes", "k8s", "k8", "kube"]),
    CanonicalSkill("devops-cicd", "CI/CD", "Cloud & DevOps", ["ci/cd", "ci-cd", "continuous integration", "continuous deployment", "jenkins", "github actions", "gitlab ci"]),
    CanonicalSkill("devops-terraform", "Terraform", "Cloud & DevOps", ["terraform", "iac", "infrastructure as code"]),
    CanonicalSkill("devops-linux", "Linux", "Cloud & DevOps", ["linux", "ubuntu", "debian", "centos", "redhat", "bash", "shell scripting"]),

    # Messaging & Distributed Systems
    CanonicalSkill("dist-kafka", "Apache Kafka", "Distributed Systems", ["kafka", "apache kafka"]),
    CanonicalSkill("dist-rabbitmq", "RabbitMQ", "Distributed Systems", ["rabbitmq", "rabbit mq"]),
    CanonicalSkill("arch-microservices", "Microservices", "Architecture", ["microservices", "microservice", "micro-services", "service oriented architecture", "soa"]),
    CanonicalSkill("api-rest", "REST APIs", "Architecture", ["rest", "rest api", "restful", "restful api", "restful apis", "web api"]),
    CanonicalSkill("api-graphql", "GraphQL", "Architecture", ["graphql"]),
    CanonicalSkill("api-grpc", "gRPC", "Architecture", ["grpc"]),

    # AI, ML & Data Science
    CanonicalSkill("ml-pytorch", "PyTorch", "Data & AI", ["pytorch", "torch"]),
    CanonicalSkill("ml-tensorflow", "TensorFlow", "Data & AI", ["tensorflow", "tf", "keras"]),
    CanonicalSkill("ml-scikit-learn", "Scikit-Learn", "Data & AI", ["scikit-learn", "sklearn"]),
    CanonicalSkill("ml-nlp", "Natural Language Processing (NLP)", "Data & AI", ["nlp", "natural language processing", "spacy", "nltk", "transformers", "huggingface", "sentence transformers", "bert", "llm", "large language models"]),
    CanonicalSkill("data-pandas", "Pandas", "Data & AI", ["pandas"]),
    CanonicalSkill("data-numpy", "NumPy", "Data & AI", ["numpy"]),

    # Version Control & Testing
    CanonicalSkill("tool-git", "Git", "Developer Tools", ["git", "github", "gitlab", "bitbucket"]),
    CanonicalSkill("test-unit", "Unit Testing", "Testing", ["junit", "pytest", "mockito", "jest", "unit testing", "tdd", "test driven development"])
]


class SkillTaxonomy:
    """
    Thread-safe lookup, alias mapping, and normalization engine for technical skills.
    """

    def __init__(self, version: str = TAXONOMY_VERSION):
        self.version = version
        self._skills_by_id: Dict[str, CanonicalSkill] = {}
        self._alias_to_skill: Dict[str, CanonicalSkill] = {}
        self._all_canonical_names: Set[str] = set()

        for skill in CANONICAL_SKILLS:
            self._skills_by_id[skill.id] = skill
            self._all_canonical_names.add(skill.name.lower())

            # Index canonical name as an alias
            self._alias_to_skill[skill.name.lower()] = skill

            # Index all explicit aliases
            for alias in skill.aliases:
                self._alias_to_skill[alias.lower().strip()] = skill

    @property
    def skill_count(self) -> int:
        return len(self._skills_by_id)

    def get_all_canonical_skills(self) -> List[CanonicalSkill]:
        return list(self._skills_by_id.values())

    def get_all_aliases(self) -> List[str]:
        return list(self._alias_to_skill.keys())

    def normalize(self, term: str) -> Optional[CanonicalSkill]:
        """
        Normalizes a raw extracted term or phrase to its canonical skill definition.
        Returns None if not recognized in the taxonomy.
        """
        if not term:
            return None
        cleaned = term.lower().strip()
        # Direct alias lookup
        if cleaned in self._alias_to_skill:
            return self._alias_to_skill[cleaned]

        # Punctuation / suffix stripping (e.g. "React.js," -> "react.js")
        stripped = cleaned.strip(".,;:|/()-")
        if stripped in self._alias_to_skill:
            return self._alias_to_skill[stripped]

        return None


# Global singleton instance
skill_taxonomy = SkillTaxonomy()
