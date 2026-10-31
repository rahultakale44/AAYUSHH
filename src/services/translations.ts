// Multi-language support for AAYUSH
import type { Language } from '../types';

type TranslationKey = 
  | 'appTitle'
  | 'appTagline'
  | 'selectRole'
  | 'seniorCitizen'
  | 'emergencyResponse'
  | 'transportPartner'
  | 'hospitalOperations'
  | 'emergencyAssistance'
  | 'patientDetails'
  | 'name'
  | 'age'
  | 'allergies'
  | 'emergencyContact'
  | 'symptoms'
  | 'symptomsPlaceholder'
  | 'voiceInput'
  | 'reviewRequest'
  | 'backToHome'
  | 'confirmEmergency'
  | 'sessionCreated'
  | 'contactEmergency'
  | 'demoWarning'
  | 'language'
  | 'heroTitle'
  | 'heroSubtitle'
  | 'requestEmergency'
  | 'learnMore'
  | 'professionalPlatform'
  | 'completeCoordination'
  | 'platformDescription';

const translations: Record<Language, Record<TranslationKey, string>> = {
  en: {
    appTitle: 'AAYUSH (आयुष)',
    appTagline: 'Senior-Friendly Emergency Assistance Platform',
    selectRole: 'Select Your Role',
    seniorCitizen: 'Senior Citizen',
    emergencyResponse: 'Emergency Response',
    transportPartner: 'Transport Partner',
    hospitalOperations: 'Hospital Operations',
    emergencyAssistance: 'Emergency Assistance',
    patientDetails: 'Patient Details',
    name: 'Full Name',
    age: 'Age',
    allergies: 'Known Allergies',
    emergencyContact: 'Emergency Contact Number',
    symptoms: 'Describe Your Symptoms',
    symptomsPlaceholder: 'Example: I have chest pain and feeling breathless...',
    voiceInput: 'Use Voice Input',
    reviewRequest: 'Review Emergency Request',
    backToHome: 'Back to Home',
    confirmEmergency: 'Confirm Emergency',
    sessionCreated: 'Emergency Session Created',
    contactEmergency: '⚠️ For immediate life-threatening emergencies, please call your local emergency services (108, 102, or 112 in India) directly.',
    demoWarning: '⚠️ This is a demonstration prototype. The urgency assessment is illustrative and NOT a medical diagnosis or validated triage system.',
    language: 'Language',
    heroTitle: 'Every Second Matters. Every Life Deserves Care.',
    heroSubtitle: 'Professional emergency coordination platform connecting patients, emergency response, transport services, and hospitals through intelligent triage and real-time coordination.',
    requestEmergency: 'Request Emergency Assistance',
    learnMore: 'Learn More',
    professionalPlatform: 'Professional healthcare coordination platform',
    completeCoordination: 'Complete Emergency Coordination',
    platformDescription: 'Professional platform connecting all stakeholders in the emergency care continuum'
  },
  hi: {
    appTitle: 'आयुष (AAYUSH)',
    appTagline: 'वरिष्ठ नागरिकों के लिए आपातकालीन सहायता मंच',
    selectRole: 'अपनी भूमिका चुनें',
    seniorCitizen: 'वरिष्ठ नागरिक',
    emergencyResponse: 'आपातकालीन प्रतिक्रिया',
    transportPartner: 'परिवहन साझेदार',
    hospitalOperations: 'अस्पताल संचालन',
    emergencyAssistance: 'आपातकालीन सहायता',
    patientDetails: 'रोगी विवरण',
    name: 'पूरा नाम',
    age: 'आयु',
    allergies: 'ज्ञात एलर्जी',
    emergencyContact: 'आपातकालीन संपर्क नंबर',
    symptoms: 'अपने लक्षणों का वर्णन करें',
    symptomsPlaceholder: 'उदाहरण: मुझे सीने में दर्द है और सांस लेने में तकलीफ हो रही है...',
    voiceInput: 'आवाज़ इनपुट का उपयोग करें',
    reviewRequest: 'आपातकालीन अनुरोध की समीक्षा करें',
    backToHome: 'होम पर वापस जाएं',
    confirmEmergency: 'आपातकाल की पुष्टि करें',
    sessionCreated: 'आपातकालीन सत्र बनाया गया',
    contactEmergency: '⚠️ तत्काल जीवन-धमकी देने वाली आपात स्थितियों के लिए, कृपया अपनी स्थानीय आपातकालीन सेवाओं (भारत में 108, 102, या 112) को सीधे कॉल करें।',
    demoWarning: '⚠️ यह एक प्रदर्शन प्रोटोटाइप है। तात्कालिकता मूल्यांकन उदाहरणात्मक है और यह चिकित्सा निदान या मान्य ट्राइएज प्रणाली नहीं है।',
    language: 'भाषा',
    heroTitle: 'हर सेकंड मायने रखता है। हर जीवन देखभाल का हकदार है।',
    heroSubtitle: 'पेशेवर आपातकालीन समन्वय मंच जो रोगियों, आपातकालीन प्रतिक्रिया, परिवहन सेवाओं और अस्पतालों को बुद्धिमान ट्राइएज और वास्तविक समय समन्वय के माध्यम से जोड़ता है।',
    requestEmergency: 'आपातकालीन सहायता का अनुरोध करें',
    learnMore: 'और जानें',
    professionalPlatform: 'पेशेवर स्वास्थ्य देखभाल समन्वय मंच',
    completeCoordination: 'पूर्ण आपातकालीन समन्वय',
    platformDescription: 'आपातकालीन देखभाल निरंतरता में सभी हितधारकों को जोड़ने वाला पेशेवर मंच'
  },
  mr: {
    appTitle: 'आयुष (AAYUSH)',
    appTagline: 'ज्येष्ठ नागरिकांसाठी आपत्कालीन सहाय्य मंच',
    selectRole: 'तुमची भूमिका निवडा',
    seniorCitizen: 'ज्येष्ठ नागरिक',
    emergencyResponse: 'आपत्कालीन प्रतिसाद',
    transportPartner: 'वाहतूक भागीदार',
    hospitalOperations: 'रुग्णालय संचालन',
    emergencyAssistance: 'आपत्कालीन सहाय्य',
    patientDetails: 'रुग्ण तपशील',
    name: 'पूर्ण नाव',
    age: 'वय',
    allergies: 'ज्ञात ऍलर्जी',
    emergencyContact: 'आपत्कालीन संपर्क क्रमांक',
    symptoms: 'तुमच्या लक्षणांचे वर्णन करा',
    symptomsPlaceholder: 'उदाहरण: मला छातीत दुखत आहे आणि श्वास घेण्यास त्रास होत आहे...',
    voiceInput: 'आवाज इनपुट वापरा',
    reviewRequest: 'आपत्कालीन विनंतीचे पुनरावलोकन करा',
    backToHome: 'मुख्यपृष्ठावर परत या',
    confirmEmergency: 'आपत्कालीन पुष्टी करा',
    sessionCreated: 'आपत्कालीन सत्र तयार केले',
    contactEmergency: '⚠️ तात्काळ जीवघेण्या आपत्कालीन परिस्थितीसाठी, कृपया तुमच्या स्थानिक आपत्कालीन सेवांना (भारतात 108, 102, किंवा 112) थेट कॉल करा।',
    demoWarning: '⚠️ हे एक प्रात्यक्षिक प्रोटोटाइप आहे। तातडीचे मूल्यांकन उदाहरणात्मक आहे आणि वैद्यकीय निदान किंवा वैध ट्रायज प्रणाली नाही।',
    language: 'भाषा',
    heroTitle: 'प्रत्येक सेकंद महत्त्वाचा आहे। प्रत्येक जीवन काळजीला पात्र आहे।',
    heroSubtitle: 'व्यावसायिक आपत्कालीन समन्वय व्यासपीठ जे रुग्ण, आपत्कालीन प्रतिसाद, वाहतूक सेवा आणि रुग्णालयांना बुद्धिमान ट्रायज आणि रिअल-टाइम समन्वयाद्वारे जोडते।',
    requestEmergency: 'आपत्कालीन सहाय्यासाठी विनंती करा',
    learnMore: 'अधिक जाणून घ्या',
    professionalPlatform: 'व्यावसायिक आरोग्य सेवा समन्वय व्यासपीठ',
    completeCoordination: 'संपूर्ण आपत्कालीन समन्वय',
    platformDescription: 'आपत्कालीन काळजी सातत्यातील सर्व भागधारकांना जोडणारे व्यावसायिक व्यासपीठ'
  }
};

export function t(key: TranslationKey, lang: Language = 'en'): string {
  return translations[lang][key] || translations.en[key];
}
