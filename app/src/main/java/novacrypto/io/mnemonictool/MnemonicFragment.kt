package novacrypto.io.mnemonictool

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import io.github.novacrypto.base58.Base58.base58Encode
import io.github.novacrypto.bip32.PrivateKey
import io.github.novacrypto.bip32.networks.Bitcoin
import io.github.novacrypto.bip39.MnemonicGenerator
import io.github.novacrypto.bip39.SeedCalculator
import io.github.novacrypto.bip39.Words
import io.github.novacrypto.bip39.wordlists.English
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import java.security.SecureRandom
import java.util.*


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [BlankFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [BlankFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class MnemonicFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var mParam1: String? = null
    private var mParam2: String? = null
    private var mListener: OnFragmentInteractionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val arguments = arguments
        if (arguments != null) {
            mParam1 = arguments.getString(ARG_PARAM1)
            mParam2 = arguments.getString(ARG_PARAM2)
        }
    }

    private var textView: TextView? = null
    private var addresses: TextView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_mnemonic, container, false)
        textView = view.findViewById(R.id.mnemonic)
        addresses = view.findViewById(R.id.addresses)

        refresh()
        view.findViewById<Button>(R.id.button).setOnClickListener { refresh() }
        return view
    }


    private val updateAddresses = CompositeDisposable()

    private fun refresh() {
        val entropy = SecureRandom().generateSeed(Words.TWELVE.byteLength())
        textView?.text = createDisplayMnemonic(entropy)
        updateAddresses.clear()
        updateAddresses += Flowable.just(formatAddresses("calculating", "calculating", "calculating")).concatWith(
                Flowable.just(createPureMnemonic(entropy))
                        .map { SeedCalculator().calculateSeed(it, "") }
                        .map { PrivateKey.fromSeed(it, Bitcoin.MAIN_NET) }
                        .map {
                            val firstAddress = base58Encode(it.derive("m/44'/0'/0'/0/0").neuter().p2pkhAddress())
                            val firstChange = base58Encode(it.derive("m/44'/0'/0'/1/0").neuter().p2pkhAddress())
                            val firstP2SH = base58Encode(it.derive("m/49'/0'/0'/0/0").neuter().p2shAddress())
                            formatAddresses(firstAddress, firstChange, firstP2SH)
                        }
        )
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    addresses?.text = it
                }
    }

    private fun formatAddresses(
            firstAddress: CharSequence?,
            firstChange: CharSequence?,
            firstP2SH: CharSequence?) = "m/49'/0'/0'/0/0\n" +
                    "$firstP2SH\n\n" +
                    "m/44'/0'/0'/0/0\n" +
                    "$firstAddress\n\n" +
                    "m/44'/0'/0'/1/0\n" +
                    "$firstChange"

    private fun createDisplayMnemonic(entropy: ByteArray): String =
            StringBuilder().apply {
                var x = 0
                MnemonicGenerator(English.INSTANCE)
                        .createMnemonic(entropy) { string ->
                            if (x % 6 == 0) appendLineHeader(x / 2 + 1)
                            append(string)
                            if (++x % 6 == 0) append('\n')
                        }
            }.toString()

    private fun createPureMnemonic(entropy: ByteArray): String =
            StringBuilder().apply {
                MnemonicGenerator(English.INSTANCE)
                        .createMnemonic(entropy) { append(it) }
            }.toString()

    private fun StringBuilder.appendLineHeader(word: Int) {
        append("$word-${word + 2}: ")
    }

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri) {
        mListener?.onFragmentInteraction(uri)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
//        if (context is OnFragmentInteractionListener) {
//            mListener = context
//        } else {
//            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
//        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment BlankFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String): MnemonicFragment {
            return MnemonicFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
        }
    }
}
