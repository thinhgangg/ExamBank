package com.exam.jlpt;

import okhttp3.*;
import com.google.gson.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;


public class GeminiClient {
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";
    private final String apiKey;
    private final OkHttpClient client;
    private final Gson gson;

    public GeminiClient(String apiKey) {
        this.apiKey = apiKey;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }

    public String getAnswerSuggestion(String question, String questionType) throws IOException {
        String finalPrompt;

        // Instructions for the AI based on the question type, written entirely in English
        switch (questionType) {
            case "Language_KanjiReading":
            case "Language_Writing":
                // Find the marked word (e.g., "くるま" from "__くるま__")
                int kwrStartMarker = question.indexOf("__");
                int kwrEndMarker = question.indexOf("__", kwrStartMarker + 2);
                if (kwrStartMarker == -1 || kwrEndMarker == -1 || kwrEndMarker <= kwrStartMarker + 2) {
                    System.err.println("Error: Marked word not found or invalid format for type: " + questionType);
                    throw new IllegalArgumentException("Input text must contain a word marked with __double_underscores__ for type " + questionType);
                }
                String kwrMarkedWord = question.substring(kwrStartMarker + 2, kwrEndMarker);
                String kwrCleanedQuestionText = question.replace("__" + kwrMarkedWord + "__", kwrMarkedWord).trim(); // Remove markers, trim whitespace

                String kanjiReadingInstructions = "Generate four Hiragana options as possible readings for the target word in this context. Include the correct reading and three incorrect readings that sound similar or are plausible mistakes, specifically focusing on creating options that are phonetically similar or commonly confused Hiragana words with different meanings.";
                String kanjiWritingInstructions = "Generate four Kanji options as possible ways to write the target word in this context. Include the correct Kanji and three incorrect Kanji options that look similar, share components, or are plausible mistakes for this context, specifically focusing on creating options that are visually similar or commonly confused Kanji with different meanings.";

                finalPrompt = String.format(
                        "Task: Provide 4 multiple-choice options for a Japanese test item of type '%s'.\n" +
                                "Context Sentence: \"%s\"\n" +
                                "Target Word/Phrase (Expected %s corresponds to this %s): `%s`\n" +
                                "Instructions: %s\n" +
                                "Format: List only the 4 options, one per line. Place the correct/most appropriate option on the first line. DO NOT include any numbering, bullet points, or extra text before the options.\n",
                        questionType,
                        kwrCleanedQuestionText,
                        questionType.equals("Language_KanjiReading") ? "Kanji" : "Hiragana/Katakana",
                        questionType.equals("Language_KanjiReading") ? "Hiragana/Katakana reading" : "Kanji form",
                        kwrMarkedWord,
                        questionType.equals("Language_KanjiReading") ? kanjiReadingInstructions : kanjiWritingInstructions
                );
                break;


            case "Language_Context":
            case "Grammar_Sentence1": // Note: Grammar_Sentence1 is distinct from Grammar_Sentence2 in structure
                // Few-shot examples for Context/Grammar (Japanese content remains), using '___'
                String grammarContextExamples =
                        "Sentence with Blank: 「（電話で）本田　「はい、　本田です。」北山　「あ、　北山花子です。　すみません。　___。」本田　「はい、　ちょっと　まって　くださいね。」」\n" +
                                "Options:\n" +
                                "ひろこさんを　おねがいします\n" + // Correct
                                "ひろこさんと　話しませんか\n" +
                                "ひろこさんを　話しますか\n" +
                                "ひろこさんを　ください\n\n" +

                                "Sentence with Blank: 「先週、おばさんの　いえ___　行きました。」\n" +
                                "Options:\n" +
                                "に\n" + // Correct particle
                                "を\n" +
                                "で\n" +
                                "と\n\n" +

                                "Sentence with Blank: 「___、もう　あるけません。」\n" +
                                "Options:\n" +
                                "つかれてから\n" + // Correct grammar pattern
                                "つかれながら\n" +
                                "つかれないで\n" +
                                "つかれて\n\n" +

                                "Sentence with Blank: 「きのうは　はれて、とても　___です。」\n" +
                                "Options:\n" +
                                "あたたかかったです\n" + // Correct conjugation (あたたかかった is also plausible depending on standard)
                                "あたたかい\n" +
                                "あたたかく\n" +
                                "あたたかいかったです\n\n" + // Grammatically incorrect

                                "Sentence with Blank: 「しょくどう___、パーティーを　しました。」\n" +
                                "Options:\n" +
                                "で\n" + // Correct particle for location of action
                                "が\n" +
                                "と\n" +
                                "に\n\n" + // に for destination, not location of action

                                "Sentence with Blank: 「日よう日は　いえで、おんがくを　聞いたり、プールで　___　します。」\n" +
                                "Options:\n" +
                                "およいだり\n" + // Correct た form
                                "およったり\n" +
                                "およいたり\n" +
                                "およんだり\n\n" +

                                "Sentence with Blank: 「今日の　テストは___。」\n" +
                                "Options:\n" +
                                "やさしくなかったです\n" + // Correct negative past
                                "やさしいでした\n" +
                                "やさしくありませんでした\n" + // Also correct, but using the example style
                                "やさしくないでした\n\n" + // Grammatically incorrect

                                "Sentence with Blank: 「とけいを　___　から　時間が　わかりません。」\n" +
                                "Options:\n" +
                                "もっていない\n" + // Correct form
                                "もつではない\n" +
                                "もたなくても\n" +
                                "もつ\n\n" +

                                "Sentence with Blank: 「つくえには　フォーク　___　スプーンも　ありませんでした。」\n" +
                                "Options:\n" +
                                "も\n" + // Correct particle for "not even"
                                "な\n" +
                                "ど\n" +
                                "か\n\n" +

                                "Sentence with Blank: 「この　えいがは　___　から　見るのを　やめましょう。」\n" +
                                "Options:\n" +
                                "おもしろくない\n" + // Correct negative i-adjective
                                "おもしろくじゃない\n" + // Grammatically incorrect
                                "おもしろいじゃない\n" + // Grammatically incorrect
                                "おもしろくなくて\n\n" + // Correct as connective, but not as sentence ender

                                "Sentence with Blank: 「今日は　かぜが　ふいて　すずしいです___。」\n" +
                                "Options:\n" +
                                "ね\n" + // Correct sentence-ending particle
                                "の\n" +
                                "が\n" +
                                "も\n\n" +

                                "Sentence with Blank: 「まだ　しゅくだいを　___　から、はやく　いえに　かえります。」\n" +
                                "Options:\n" +
                                "して　いない\n" + // Correct ている negative
                                "しないではない\n" +
                                "しないで\n" +
                                "しました\n\n" +

                                "Sentence with Blank: 「毎あさ、ごはんを　___　あとで、シャワーを　あびます。」\n" +
                                "Options:\n" +
                                "食べた\n" + // Correct た form + あとで
                                "食べました\n" +
                                "食べて\n" +
                                "食べる\n\n" +

                                "Sentence with Blank: 「すみません、ゆうびんきょくは___ですか。」\n" +
                                "Options:\n" +
                                "どこ\n" + // Correct question word for location
                                "いくら\n" +
                                "どんな\n" +
                                "どの\n\n" +

                                "Sentence with Blank: 「会社___、６時半に　出ました。」\n" +
                                "Options:\n" +
                                "を\n" + // Correct particle for leaving a place
                                "まで\n" +
                                "に\n" +
                                "と\n\n";


                finalPrompt = String.format(
                        "Provide 4 multiple-choice options for the blank in the Japanese sentence below, suitable for a '%s' test item.\n" +
                                "Input: A Japanese sentence containing a blank indicated by three underscores (__).\n" +
                                "The options should be single words or short phrases in Hiragana, Katakana, or common Kanji.\n" +
                                "Instructions:\n" +
                                "1. The first option must be the word or phrase that fits the blank grammatically and semantically in the context of the sentence. (Find the word most suitable semantically and grammatically based on the context).\n" +
                                "2. Generate three incorrect options that are plausible distractors for this blank. They should ideally belong to the same grammatical category and be either grammatically possible elsewhere but semantically inappropriate here, or common grammatical/vocabulary confusion points. (Create 3 incorrect options that may be grammatically correct but semantically wrong or easily confusing).\n" +
                                "3. List ONLY the 4 options, one per line. Place the correct option FIRST, and NO numbering, bullet points, or extra text.\n\n" +
                                "--- Examples ---\n\n" +
                                grammarContextExamples +
                                "--- End Examples ---\n\n" +

                                "Sentence with Blank: 「%s」\n" +
                                "Options:",
                        questionType,
                        question.trim()
                );
                break;

            case "Grammar_Sentence2": // Sentence Recombination (もんだい２ in JLPT)
                // Input format from ExamBankApp is expected to be:
                // "Structure: <multi-line structure text>\nSegments: [<Seg1>, <Seg2>, ...]"

                // Parse the input string to extract structure and segments
                String[] parts = question.split("\nSegments: \\[", 2); // Split into structure part and segments part
                if (parts.length != 2) {
                    System.err.println("Input parsing error for Grammar_Sentence2 in GeminiClient: Missing 'Segments: [' separator. Input: " + question);
                    throw new IllegalArgumentException("Input format error for Grammar_Sentence2 in AI call.");
                }

                String structureSection = parts[0]; // "Structure: <multi-line text>"
                String segmentsSection = parts[1]; // "<Seg1>, <Seg2>, ...]""

                if (!structureSection.startsWith("Structure: ")) {
                    System.err.println("Input parsing error for Grammar_Sentence2 in GeminiClient: Missing 'Structure: ' prefix. Section: " + structureSection);
                    throw new IllegalArgumentException("Input format error for Grammar_Sentence2 in AI call.");
                }
                String sentenceStructure = structureSection.substring("Structure: ".length()).trim();

                if (!segmentsSection.endsWith("]")) {
                    System.err.println("Input parsing error for Grammar_Sentence2 in GeminiClient: Missing ']' suffix in segments. Section: " + segmentsSection);
                    throw new IllegalArgumentException("Input format error for Grammar_Sentence2 in AI call.");
                }
                String segmentsString = segmentsSection.substring(0, segmentsSection.length() - 1); // Remove trailing ']'


                List<String> segments = Arrays.stream(segmentsString.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty()) // Filter out potential empty strings from split like "a,,b"
                        .collect(Collectors.toList());

                if (segments.size() != 4) {
                    System.err.println("Input parsing error for Grammar_Sentence2 in GeminiClient: Expected 4 segments, found " + segments.size() + ". Segments: " + segments);
                    throw new IllegalArgumentException("Input format error for Grammar_Sentence2 in AI call: Expected 4 segments.");
                }

                // --- New Prompt Strategy ---
                finalPrompt = String.format(
                        "Task: Analyze the Japanese sentence structure and the four provided segments. Identify which *single segment* from the list of four should be placed into the position marked by 'X' to form a grammatically correct and natural-sounding complete sentence.\n" +
                                "Input:\n" +
                                "Sentence Structure: %s\n" + // Use the extracted multi-line structure here
                                "Segments: [%s]\n" + // Present segments as a list for clarity
                                "Output Format:\n" +
                                "Provide ONLY the single segment that belongs in the 'X' position. DO NOT include any other text, numbering, or the other segments.\n\n" +
                                // Add a few examples for the AI following the *new* input/output format
                                "--- Examples ---\n\n" +
                                // Example 1 (from your example - multi-line structure handled by prompt format)
                                "Sentence Structure: (店で）\n田中　「すみません。くだもの  __ __ X __  か。」\n店の　人　「こちらです。」\n" + // Include multi-line structure in example
                                "Segments: [は, に, あります, どこ]\n" +
                                "Output:\n" +
                                "に\n\n" + // Only the correct segment

                                // Example 2 (modified from a previous one, using X, single line structure)
                                "Sentence Structure: わたしは、　あさ　８時　__ __ __ X __ ます。\n" +
                                "Segments: [から, ゆうがた　６時まで, はたらき, います]\n" +
                                "Output:\n" +
                                "はたらき\n\n" +

                                // Example 3 (another modified one, single line structure)
                                "Sentence Structure: いえに　 _ _ X _　を　した。\n" +
                                "Segments: [前に, デパートで, かえる, 買いもの]\n" +
                                "Output:\n" +
                                "デパートで\n\n" +

                                "--- End Examples ---\n\n" +

                                // Placeholder for the user's input
                                "Sentence Structure: %s\n" +
                                "Segments: [%s]\n" +
                                "Output:",
                        questionType, // Added questionType to format string (can remove if not needed by prompt)
                        sentenceStructure.trim(), // Use extracted structure, trimmed
                        segments.stream().collect(Collectors.joining(", ")), // Use extracted segments, joined
                        sentenceStructure.trim(), // Repeat for the query part
                        segments.stream().collect(Collectors.joining(", ")) // Repeat for the query part
                );
                break;


            // --- Add Reading_ShortText Prompt ---
            case "Reading_ShortText":
                // Input format from ExamBankApp is expected to be:
                // "Paragraph: <multi-line paragraph text>\nQuestion: <multi-line question text>"

                // Parse the input string to extract paragraph and question
                String[] rstParts = question.split("\nQuestion: ", 2); // Split into paragraph part and question part
                if (rstParts.length != 2) {
                    System.err.println("Input parsing error for Reading_ShortText in GeminiClient: Missing 'Question: ' separator. Input: " + question);
                    throw new IllegalArgumentException("Input format error for Reading_ShortText in AI call.");
                }

                String paragraphSection = rstParts[0]; // "Paragraph: <multi-line text>"
                String questionSection = rstParts[1]; // "<multi-line question text>"

                if (!paragraphSection.startsWith("Paragraph: ")) {
                    System.err.println("Input parsing error for Reading_ShortText in GeminiClient: Missing 'Paragraph: ' prefix. Section: " + paragraphSection);
                    throw new IllegalArgumentException("Input format error for Reading_ShortText in AI call.");
                }
                String paragraphText = paragraphSection.substring("Paragraph: ".length()).trim();
                String questionText = questionSection.trim(); // Trim the question part


                finalPrompt = String.format(
                        "Task: Read the following Japanese paragraph and answer the question based on its content. Provide exactly four multiple-choice options for the question.\n" +
                                "Input:\n" +
                                "Paragraph: %s\n" + // Use extracted paragraph
                                "Question: %s\n" + // Use extracted question
                                "Instructions:\n" +
                                "1. The first option must be the correct answer to the question based *strictly* on the provided paragraph.\n" +
                                "2. Generate three incorrect options that are plausible distractors, but are definitively wrong based on the paragraph's content or are grammatically/semantically similar to the correct answer but ultimately incorrect.\n" +
                                "3. List ONLY the 4 options, one per line. Place the correct option FIRST, and NO numbering, bullet points, or extra text before the options.\n" +
                                "4. Options should be concise (single words, phrases, or short sentences) and in Hiragana, Katakana, or common Kanji if appropriate for the expected answer.\n\n" +
                                "--- Examples ---\n\n" +
                                // Example 1 (using part of your original image's text and a question)
                                "Paragraph: 日本で べんきょうしている 学生が「すきな 店」の ぶんしょうを 書いて、クラスの みんなの 前で 読みました。\nわたしは すしが すきです。日本には たくさん すし屋が ありますね。わたしの 国には すし屋が ありませんから、今 とても うれしいです。日本に、いろいろな 店で 食べました。学校の 前の 店は、安くて おいしいです。\n" +
                                "Question: このぶんしょうを 書いた人は、どうしてうれしいですか。\n" +
                                "Output:\n" +
                                "日本に すし屋が たくさん あるから\n" + // Correct (implied by text)
                                "日本の すしは 安いから\n" + // Plausible but not main reason for *being happy*
                                "日本の 学校の 前に おいしい 店があるから\n" + // Plausible but not main reason for *being happy*
                                "自分の 国にも すし屋が あるから\n\n" + // Incorrect (text says ない)

                                // Add more diverse examples as needed for better AI performance

                                "--- End Examples ---\n\n" +
                                // Placeholder for the user's input
                                "Paragraph: %s\n" + // Use extracted paragraph for query part
                                "Question: %s\n" + // Use extracted question for query part
                                "Output:",
                        questionType, // Add questionType (can remove if not needed by prompt)
                        paragraphText, // Use extracted paragraph text
                        questionText, // Use extracted question text
                        paragraphText, // Repeat for query part
                        questionText // Repeat for query part
                );
                break; // End Reading_ShortText case
            // --- End Add Reading_ShortText Prompt ---


            case "Language_Synonyms":
                // Few-shot examples for Synonyms (Japanese content remains, Kanij removed)
                String synonymExamples =
                        "Original Sentence: 「まいばん　くにの　かぞくに　でんわします。」\n" +
                                "Options:\n" +
                                "よるは　いつも　くにの　かぞくに　でんわします。\n" + // Correct
                                "あさは　いつも　くにの　かぞくに　でんわします。\n" + // Incorrect (time)
                                "あさは　ときどき　くにの　かぞくに　でんわします。\n" + // Incorrect (time and freq)
                                "よるは　ときどき　くにの　かぞくに　でんわします。\n\n" + // Incorrect (freq)

                                "Original Sentence: 「この　まちには　ゆうめいな　たてものが　あります。」\n" +
                                "Options:\n" +
                                "この　まちには　ゆうめいな　ビルが　あります。\n" + // Correct
                                "この　まちには　ゆうめいな　こうえんが　あります。\n" + // Incorrect (different topic)
                                "この　まちには　ゆうめいな　ケーキが　あります。\n" + // Incorrect (different topic)
                                "この　まちには　ゆうめいな　おちゃが　あります。\n\n" + // Incorrect (different topic)

                                "Original Sentence: 「その　えいがは　おもしろくなかったです。」\n" +
                                "Options:\n" +
                                "その　えいがは　つまらなかったです。\n" + // Correct
                                "その　えいがは　ながかったです。\n" +     // Incorrect (different property)
                                "その　えいがは　みじかかったです。\n" +   // Incorrect (different property)
                                "その　えいがは　たのしかったです。\n\n" +   // Incorrect (opposite meaning)

                                "Original Sentence: 「にねんまえに　きょうとへ　いきました。」\n" +
                                "Options:\n" +
                                "おととし　きょうとへ　いきました。\n" + // Correct
                                "きょねん　きょうとへ　いきました。\n" +     // Incorrect (different time)
                                "おととい　きょうとへ　いきました。\n" +   // Incorrect (different time)
                                "きのう　きょうとへ　いきました。\n\n" +     // Incorrect (different time)

                                "Original Sentence: 「わたしの　たんじょうびは　６がつ１５にちです。」\n" +
                                "Options:\n" +
                                "６がつ１５にちに　うまれました。\n" + // Correct
                                "６がつ１５にちに　くにへ　かえりました。\n" + // Incorrect (Different event)
                                "６がつ１５にちに　テストが　はじまりました。\n" + // Incorrect (Different event)
                                "６がつ１５にちに　けっこんしました。\n\n";


                finalPrompt = String.format(
                        "Task: Provide 4 multiple-choice sentence options for a Japanese test item of type '%s' (Sentence Synonyms).\n" +
                                "Input: An original Japanese sentence.\n" +
                                "Output: 4 Japanese sentences.\n" +
                                "Instructions:\n" +
                                "1. The first output sentence must be a close synonym or have the same meaning as the input sentence. (Find expressions or words close in meaning to the original sentence).\n" +
                                "2. Generate three other sentences that have meanings different from the input sentence, but look or sound similar to the original or correct sentence by changing a key word or phrase while maintaining a similar sentence structure or appearance. (Create 3 other sentences with similar writing/structure but different meanings).\n" +
                                "3. CRITICAL: DO NOT use Kanji in ANY of the 4 generated option sentences. Use only Hiragana or Katakana.\n" +
                                "4. List ONLY the 4 sentences, one per line, with the correct synonym sentence FIRST, and NO numbering, bullet points, or extra text.\n\n" +
                                "--- Examples ---\n\n" +
                                synonymExamples +
                                "--- End Examples ---\n\n" +

                                "Original Sentence: 「%s」\n" +
                                "Options:",
                        questionType,
                        question.trim()
                );
                break;


            // Reading_LongText, Reading_InfoSearch, Listening types are handled by ExamBankApp early exit

            default:
                // Fallback prompt for any other types not explicitly handled by AI suggestion logic
                System.err.println("Using generic fallback prompt for unhandled question type: " + questionType);
                finalPrompt = String.format(
                        "Task: Provide 4 multiple-choice options for a Japanese test item of type '%s'.\n" +
                                "Japanese Text: \"%s\"\n" +
                                "Instructions: Generate four relevant options based on the provided text and type. Include the correct option and three plausible incorrect options.\n" + // Removed blank/X hint
                                "Format: List only the 4 options, one per line. Place the most appropriate option first. DO NOT include any numbering, bullet points, or extra text before the options.\n",
                        questionType,
                        question.trim()
                );
                break;
        }

        // --- API Call Setup ---
        JsonObject textPart = new JsonObject();
        textPart.addProperty("text", finalPrompt);

        JsonArray partsArray = new JsonArray();
        partsArray.add(textPart);

        JsonObject userContent = new JsonObject();
        userContent.addProperty("role", "user");
        userContent.add("parts", partsArray);

        JsonArray contents = new JsonArray();
        contents.add(userContent);

        JsonObject generationConfig = new JsonObject();
        // Adjusted max tokens based on types
        int maxTokens = 500;
        if (questionType.equals("Language_Synonyms") || questionType.equals("Reading_ShortText")) { // RST might need more tokens for paragraph/question
            maxTokens = 1200;
        } else if (questionType.equals("Language_Context") || questionType.equals("Grammar_Sentence1")) {
            maxTokens = 1800;
        } else if (questionType.equals("Grammar_Sentence2")) {
            maxTokens = 300;
        }
        if (maxTokens < 300) maxTokens = 300;


        generationConfig.addProperty("maxOutputTokens", maxTokens);

        // Safety settings (optional, configure as needed)
        JsonArray safetySettings = new JsonArray();
        JsonObject harmNone = new JsonObject();
        harmNone.addProperty("category", "HARM_CATEGORY_DANGEROUS_CONTENT");
        harmNone.addProperty("threshold", "BLOCK_NONE");
        safetySettings.add(harmNone);
        JsonObject harmSex = new JsonObject();
        harmSex.addProperty("category", "HARM_CATEGORY_SEXUALLY_EXPLICIT");
        harmSex.addProperty("threshold", "BLOCK_NONE");
        safetySettings.add(harmSex);
        JsonObject harmHate = new JsonObject();
        harmHate.addProperty("category", "HARM_CATEGORY_HATE_SPEECH");
        harmHate.addProperty("threshold", "BLOCK_NONE");
        safetySettings.add(harmHate);
        JsonObject harmHarass = new JsonObject();
        harmHarass.addProperty("category", "HARM_CATEGORY_HARASSMENT");
        harmHarass.addProperty("threshold", "BLOCK_NONE");
        safetySettings.add(harmHarass);


        JsonObject jsonBody = new JsonObject();
        jsonBody.add("contents", contents);
        jsonBody.add("generationConfig", generationConfig);
        jsonBody.add("safetySettings", safetySettings); // Add safety settings


        System.out.println("Request JSON body: " + jsonBody.toString());

        RequestBody body = RequestBody.create(
                jsonBody.toString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(API_URL + "?key=" + apiKey)
                .post(body)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();

            if (!response.isSuccessful()) {
                throw new IOException("API Error. Code: " + response.code() + ", Message: " + response.message() + ", Body: " + responseBody);
            }

            try {
                JsonObject jsonObject = gson.fromJson(responseBody, JsonObject.class);

                JsonArray candidates = jsonObject.getAsJsonArray("candidates");
                if (candidates == null || candidates.size() == 0) {
                    JsonObject promptFeedback = jsonObject.getAsJsonObject("promptFeedback");
                    String feedback = "No candidates generated.";
                    if (promptFeedback != null) {
                        if (promptFeedback.has("blockReason")) {
                            feedback += " Blocked Reason: " + promptFeedback.get("blockReason").getAsString();
                        }
                        if (promptFeedback.has("safetyRatings")) {
                            feedback += ", Safety Ratings: " + promptFeedback.get("safetyRatings").toString();
                        }
                    }
                    // Throw a more informative error if the response indicates blocking
                    if (promptFeedback != null && promptFeedback.has("blockReason")) {
                        throw new IOException("API Blocked Request: " + promptFeedback.get("blockReason").getAsString() + ". Response Body: " + responseBody);
                    }

                    throw new IOException("API returned success but no candidates. " + feedback + " Response Body: " + responseBody);
                }
                JsonObject firstCandidate = candidates.get(0).getAsJsonObject();
                if (firstCandidate.has("finishReason") && !"STOP".equals(firstCandidate.get("finishReason").getAsString())) {
                    System.err.println("Warning: API finished with reason: " + firstCandidate.get("finishReason").getAsString() + " - Response may be incomplete.");
                }


                JsonObject contentResponse = firstCandidate.getAsJsonObject("content");
                if (contentResponse == null) {
                    JsonObject promptFeedback = jsonObject.getAsJsonObject("promptFeedback");
                    String feedback = "";
                    if(promptFeedback != null && promptFeedback.has("blockReason")) {
                        feedback = " Reason: " + promptFeedback.get("blockReason").getAsString();
                    }
                    if(promptFeedback != null && promptFeedback.has("safetyRatings")) {
                        feedback += ", Safety Ratings: " + promptFeedback.toString();
                    }
                    throw new IOException("API response missing 'content' (potentially blocked or no response generated)." + feedback + " Response Body: " + responseBody);
                }


                JsonArray parts = contentResponse.getAsJsonArray("parts");
                if (parts == null || parts.size() == 0) {
                    throw new IOException("API response missing 'parts'. Response Body: " + responseBody);
                }
                JsonObject textPartResponse = parts.get(0).getAsJsonObject();
                if (textPartResponse == null) {
                    throw new IOException("API response missing first part object. Response Body: " + responseBody);
                }
                JsonElement textElement = textPartResponse.get("text");
                if (textElement == null || !textElement.isJsonPrimitive() || !textElement.getAsJsonPrimitive().isString()) {
                    throw new IOException("API response missing 'text' in first part or it's not a string. Response Body: " + responseBody);
                }


                String answer = textElement.getAsString();

                // Special handling for Grammar_Sentence2 response (returns 1 segment)
                if (questionType.equals("Grammar_Sentence2")) {
                    // The AI should return ONLY the correct segment.
                    // Trim whitespace and return the first line if there are multiple (shouldn't be).
                    String[] responseLines = answer.trim().split("\\r?\\n");
                    if (responseLines.length == 0 || responseLines[0].trim().isEmpty()) {
                        System.err.println("AI returned empty or invalid response format for Grammar_Sentence2: " + answer);
                        // Return empty string to indicate failure to find segment
                        return "";
                    }
                    return responseLines[0].trim(); // Return only the identified correct segment
                }

                // For other types (including Reading_ShortText), return the raw text containing options
                return answer.trim();
            } catch (JsonSyntaxException e) {
                throw new IOException("API JSON parsing error: " + e.getMessage() + ". Response Body: " + responseBody, e);
            }
        }
    }
}