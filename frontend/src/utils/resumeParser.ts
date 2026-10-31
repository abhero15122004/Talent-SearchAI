/**
 * TalentMatch AI - Intelligent Resume Text Structuring & Normalization Utility
 * Transforms unformatted, flattened, or noisy resume extraction text into structured,
 * professional executive presentations with clean bullet points and distinct entity cards.
 */

export interface ParsedJobExperience {
  roleTitle: string;
  companyName?: string;
  location?: string;
  dateRange?: string;
  bullets: string[];
}

export interface ParsedEducation {
  degree: string;
  institution?: string;
  location?: string;
  year?: string;
  gpa?: string;
}

export interface ParsedHonorsOrActivity {
  title: string;
  details: string[];
}

export interface ParsedResumeLinks {
  website?: string;
  linkedin?: string;
  github?: string;
  otherLinks: string[];
}

export interface StructuredResumeProfile {
  experiences: ParsedJobExperience[];
  educations: ParsedEducation[];
  honors: string[];
  activities: string[];
  links: ParsedResumeLinks;
}

/**
 * Normalizes mojibake and encoding artifacts commonly found in PDF/DOCX extracted text.
 */
export const sanitizeText = (text?: string | null): string => {
  if (!text) return '';
  return text
    .replace(/\u00a0/g, ' ')
    .replace(/Â/g, '')
    .replace(/"˜/g, '"')
    .replace(/˜/g, '')
    .replace(/â€™/g, "'")
    .replace(/â€œ/g, '"')
    .replace(/â€\u009d/g, '"')
    .replace(/â€/g, '"')
    .replace(/â€"/g, '—')
    .replace(/([a-zA-Z0-9])\s*,\s*([a-zA-Z0-9])/g, '$1, $2')
    .replace(/\s+/g, ' ')
    .trim();
};

/**
 * Splits a long descriptive text or paragraph into clean, high-impact bullet points.
 */
export const breakIntoBullets = (text: string, maxBullets = 12): string[] => {
  if (!text) return [];

  // If text already has explicit bullet characters, split on them
  if (/[\n\r]\s*[•\-\*]\s+/g.test(text) || text.includes('•')) {
    const rawBullets = text
      .split(/(?:[\n\r]\s*[•\-\*]\s+|•)/)
      .map((b) => sanitizeText(b))
      .filter((b) => b.length > 5);
    if (rawBullets.length > 0) return rawBullets.slice(0, maxBullets);
  }

  // Otherwise, split on sentence boundaries
  const sentences = text
    .split(/(?<=[.!?])\s+(?=[A-Z0-9"'])/)
    .map((s) => sanitizeText(s))
    .filter((s) => s.length >= 10 && !s.toLowerCase().startsWith('company name'));

  if (sentences.length === 0 && text.trim().length > 0) {
    return [sanitizeText(text)];
  }

  return sentences.slice(0, maxBullets);
};

// Date range pattern e.g. "05/2017 to 08/2017", "05/2014 - 06/2016", "Jan 2018 - Present"
const DATE_RANGE_REGEX =
  /(?:\b(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\.?\s+)?\b(\d{1,2}\/\d{4}|\d{4})\s*(?:to|-|–|—)\s*(?:(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\.?\s+)?(\d{1,2}\/\d{4}|\d{4}|Present|Current)\b/i;

/**
 * Parses flat or multi-line work experience text into structured job cards with bulleted achievements.
 */
export const parseWorkExperiences = (experienceText?: string | null): ParsedJobExperience[] => {
  if (!experienceText) return [];
  const clean = sanitizeText(experienceText);

  // Common titles to identify job boundaries when text is merged together
  const jobHeaderRegex =
    /(?:^|(?<=[.!?]\s+))([A-Z][A-Za-z0-9\s/&()-]{3,50})\s+(\d{1,2}\/\d{4}|\d{4})\s*(?:to|-|–|—)\s*(\d{1,2}\/\d{4}|\d{4}|Present|Current)\s+([A-Za-z0-9\s,.-]{2,50}?)(?=(?:\s+[A-Z][a-z]+ed|\s+Created|\s+Worked|\s+Developed|\s+Led|\s+Coded|\s+Involved|\s+Modularized|\s+Architected|\s+Designed|\s+Core|\s+Trained|\s+Integrated|\s+Built|\s+Responsible|\s+Interpreted|\s+Fixed|\s+Gained|\s+Attended|\s+Did|\s+Was|\s+Prepared|\s+Collaborated|\s+Leveraged))/gi;

  const results: ParsedJobExperience[] = [];
  const matches = [...clean.matchAll(jobHeaderRegex)];

  if (matches.length > 0) {
    for (let i = 0; i < matches.length; i++) {
      const match = matches[i];
      const roleTitle = match[1].trim();
      const startDate = match[2];
      const endDate = match[3];
      const companyLocation = match[4].trim();

      const startIndex = match.index! + match[0].length;
      const endIndex = i < matches.length - 1 ? matches[i + 1].index! : clean.length;
      const bodyText = clean.substring(startIndex, endIndex).trim();

      let company = companyLocation;
      let location: string | undefined = undefined;
      if (companyLocation.includes(',')) {
        const parts = companyLocation.split(',').map((p) => p.trim());
        company = parts[0] || 'Company';
        location = parts.slice(1).join(', ');
      }

      results.push({
        roleTitle,
        companyName: company || undefined,
        location,
        dateRange: `${startDate} — ${endDate}`,
        bullets: breakIntoBullets(bodyText),
      });
    }
    return results;
  }

  // Fallback: Split on double newlines or single newlines if structured
  const blocks = clean.split(/\n\s*\n/).filter((b) => b.trim().length > 0);
  if (blocks.length > 1) {
    return blocks.map((block) => {
      const lines = block.split('\n').map((l) => l.trim()).filter(Boolean);
      const title = lines[0] || 'Experience';
      const body = lines.slice(1).join(' ') || title;
      const dateMatch = block.match(DATE_RANGE_REGEX);

      return {
        roleTitle: title,
        dateRange: dateMatch ? dateMatch[0] : undefined,
        bullets: breakIntoBullets(body),
      };
    });
  }

  // Single block fallback:
  return [
    {
      roleTitle: 'Professional Experience',
      bullets: breakIntoBullets(clean),
    },
  ];
};

/**
 * Parses education text into distinct degrees, institutions, GPA, and graduation year.
 */
export const parseEducations = (educationText?: string | null): ParsedEducation[] => {
  if (!educationText) return [];
  const clean = sanitizeText(educationText);

  // Stop before subsequent sections if they got merged (Honours, Extra-Curricular, Links)
  const eduBoundary = clean.split(/(?:Honours?\s*(?:and|&)?\s*Distinction|Extra-?Curricular|Website\s*and\s*Links)/i)[0];

  const degreeRegex =
    /(Master(?:'s)?(?:\s+of\s+[A-Za-z]+)?|Bachelor(?:'s)?(?:\s+of\s+[A-Za-z]+)?|Ph\.?D\.?|B\.?S\.?|M\.?S\.?|B\.?Tech|M\.?Tech|B\.?E\.?|M\.?E\.?|B\.?A\.?|M\.?B\.?A\.?|Associate(?:'s)?)(?:\s*:\s*|\s+in\s+)?([A-Za-z\s&,.-]+?)(\b\d{4}\b)\s+([A-Za-z\s,.-]+?)(?=(?:GPA:|\b\d{4}\b|Bachelor|Master|Ph\.?D\.?|B\.?E\.?|M\.?S\.?|$))/gi;

  const results: ParsedEducation[] = [];
  const matches = [...eduBoundary.matchAll(degreeRegex)];

  if (matches.length > 0) {
    for (const match of matches) {
      const degreeType = match[1].trim();
      const major = match[2]?.trim().replace(/[:,-]+$/, '').trim();
      const year = match[3];
      const institutionRaw = match[4]?.trim() || '';

      // Look for GPA in nearby text
      const gpaMatch = eduBoundary.substring(match.index!, match.index! + 200).match(/GPA:\s*([0-9.]+\/[0-9.]+|[0-9.]+)/i);

      let institution = institutionRaw;
      let location: string | undefined = undefined;

      if (institutionRaw.includes(',')) {
        const parts = institutionRaw.split(',').map((p) => p.trim());
        institution = parts[0];
        location = parts.slice(1).join(', ');
      }

      results.push({
        degree: major ? `${degreeType} in ${major}` : degreeType,
        institution: institution || undefined,
        location,
        year,
        gpa: gpaMatch ? gpaMatch[1] : undefined,
      });
    }
    return results;
  }

  // Fallback: Split on lines or simple degrees
  const gpaMatch = clean.match(/GPA:\s*([0-9.]+\/[0-9.]+|[0-9.]+)/i);
  const yearMatch = clean.match(/\b(19\d{2}|20\d{2})\b/);

  return [
    {
      degree: clean.split(/(?:19\d{2}|20\d{2}|GPA)/)[0]?.trim() || 'Degree / Qualification',
      year: yearMatch ? yearMatch[0] : undefined,
      gpa: gpaMatch ? gpaMatch[1] : undefined,
    },
  ];
};

/**
 * Extracts honors, extra-curricular activities, and web links from full resume text.
 */
export const extractAdditionalResumeSections = (text?: string | null): {
  honors: string[];
  activities: string[];
  links: ParsedResumeLinks;
} => {
  const result: {
    honors: string[];
    activities: string[];
    links: ParsedResumeLinks;
  } = {
    honors: [],
    activities: [],
    links: { otherLinks: [] },
  };

  if (!text) return result;
  const clean = sanitizeText(text);

  // 1. Honors and Distinctions
  const honorsMatch = clean.match(
    /Honours?\s*(?:and|&)?\s*Distinctions?\s*([\s\S]*?)(?=(?:Extra-?Curricular|Website\s*and\s*Links|Work\s*Experience|$))/i
  );
  if (honorsMatch && honorsMatch[1]) {
    result.honors = breakIntoBullets(honorsMatch[1], 6);
  }

  // 2. Extra-Curricular Activities
  const activitiesMatch = clean.match(
    /Extra-?Curricular\s*Activities\s*([\s\S]*?)(?=(?:Website\s*and\s*Links|Work\s*Experience|Honours|$))/i
  );
  if (activitiesMatch && activitiesMatch[1]) {
    result.activities = breakIntoBullets(activitiesMatch[1], 8);
  }

  // 3. Links and Profiles
  const linkSectionMatch = clean.match(/Website\s*(?:and|&)?\s*Links\s*([\s\S]*?)$/i);
  const linksText = linkSectionMatch ? linkSectionMatch[1] : clean;

  const websiteMatch = linksText.match(/(?:Website:\s*)?(https?:\/\/[^\s]+|www\.[^\s]+\.[a-z]{2,})/i);
  const linkedinMatch = linksText.match(/(?:LinkedIn:\s*)?(?:https?:\/\/)?(?:www\.)?linkedin\.com\/in\/([a-zA-Z0-9_-]+)/i);
  const githubMatch = linksText.match(/(?:GitHub:\s*)?(?:https?:\/\/)?(?:www\.)?github\.com\/([a-zA-Z0-9_-]+)/i);

  if (websiteMatch && !websiteMatch[0].includes('linkedin') && !websiteMatch[0].includes('github')) {
    result.links.website = websiteMatch[0].replace(/^Website:\s*/i, '');
  }
  if (linkedinMatch) {
    result.links.linkedin = linkedinMatch[0].replace(/^LinkedIn:\s*/i, '');
  }
  if (githubMatch) {
    result.links.github = githubMatch[0].replace(/^GitHub:\s*/i, '');
  }

  return result;
};
