package com.example.safeher.settings.lobby

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.safeher.R
import com.example.safeher.databinding.FragmentQuestionsAndAnswersBinding

class QuestionsAndAnswersFragment : Fragment() {

    private var _binding: FragmentQuestionsAndAnswersBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuestionsAndAnswersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.backButtonCard.setOnClickListener {
            findNavController().navigate(R.id.action_questionsAndAnswersFragment_to_settingsLobbyFragment)
        }

        val htmlContent = """
            <b>What is SafeHer?</b><br>
            SafeHer is a safety-focused app designed to help women feel more secure in everyday life. It includes features like emergency contact sharing, real-time location alerts, and a customizable Safe Circle.<br><br>

            <b>How do I add someone to my Safe Circle?</b><br>
            Go to Settings → Safe Circle → Choose Contacts. You can select up to 5 trusted contacts from your phonebook who will be notified in case of emergency.<br><br>

            <b>What happens when I press the SOS button?</b><br>
            Pressing the SOS button immediately sends your real-time location to everyone in your Safe Circle, along with a predefined emergency message.<br><br>

            <b>Can I remove someone from my Safe Circle?</b><br>
            Yes. Go to the Safe Circle screen and use the "Edit" option to remove contacts.<br><br>

            <b>Do my contacts need to have the app?</b><br>
            No. They receive alerts via SMS or WhatsApp, even if they don’t have the app installed.<br><br>

            <b>Does SafeHer work without internet?</b><br>
            Basic features do, but SOS and location sharing require an internet or data connection.<br><br>

            <b>Is my personal data secure?</b><br>
            Yes. All data is securely stored and encrypted.<br><br>

            <b>How do I delete my account?</b><br>
            You can request account deletion via the “Help” section.<br><br>
        """.trimIndent()

        binding.qaText.text = HtmlCompat.fromHtml(htmlContent, HtmlCompat.FROM_HTML_MODE_LEGACY)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}