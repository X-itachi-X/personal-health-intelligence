import {
  agentFetchAbnormal,
  agentFetchAvailableBiomarkers,
  agentFetchChanges,
  agentFetchImagingContext,
  agentFetchImagingStudies,
  agentFetchMedicationContext,
  agentFetchMedications,
  agentFetchTrend,
} from "../api";
import {
  formatAbnormal,
  formatAvailableBiomarkers,
  formatChanges,
  formatHelpMessage,
  formatImagingStudies,
  formatInsightCards,
  formatMedications,
  formatTrend,
  formatUnknownMessage,
} from "./formatters";
import { AgentIntent, resolveIntent } from "./intents";

export type AgentChatResult = {
  intent: AgentIntent["type"];
  answer: string;
};

export async function runAgentQuery(personId: number, query: string): Promise<AgentChatResult> {
  const available = await agentFetchAvailableBiomarkers(personId).catch(() => []);
  const intent = resolveIntent(query, available);

  switch (intent.type) {
    case "help":
      return { intent: intent.type, answer: formatHelpMessage() };

    case "changes": {
      const changes = await agentFetchChanges(personId);
      return { intent: intent.type, answer: formatChanges(changes) };
    }

    case "medications": {
      const meds = await agentFetchMedications(personId);
      return { intent: intent.type, answer: formatMedications(meds) };
    }

    case "medication_context": {
      const cards = await agentFetchMedicationContext(personId);
      return {
        intent: intent.type,
        answer: formatInsightCards("Medication + lab context", cards),
      };
    }

    case "imaging_studies": {
      const studies = await agentFetchImagingStudies(personId);
      return { intent: intent.type, answer: formatImagingStudies(studies) };
    }

    case "imaging_context": {
      const cards = await agentFetchImagingContext(personId);
      return {
        intent: intent.type,
        answer: formatInsightCards("Imaging + lab context", cards),
      };
    }

    case "abnormal": {
      const changes = await agentFetchChanges(personId);
      if (!changes.latestReportId) {
        return {
          intent: intent.type,
          answer: "No completed lab report found to check for out-of-range results.",
        };
      }
      const findings = await agentFetchAbnormal(changes.latestReportId);
      return { intent: intent.type, answer: formatAbnormal(findings) };
    }

    case "available_biomarkers":
      return {
        intent: intent.type,
        answer: formatAvailableBiomarkers(available),
      };

    case "trend": {
      const trend = await agentFetchTrend(personId, intent.canonical);
      return { intent: intent.type, answer: formatTrend(trend) };
    }

    case "unknown":
    default:
      return { intent: "unknown", answer: formatUnknownMessage() };
  }
}
