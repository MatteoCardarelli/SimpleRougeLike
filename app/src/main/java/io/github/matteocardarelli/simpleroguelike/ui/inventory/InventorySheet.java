package io.github.matteocardarelli.simpleroguelike.ui.inventory;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import androidx.recyclerview.widget.GridLayoutManager;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import io.github.matteocardarelli.simpleroguelike.R;
import io.github.matteocardarelli.simpleroguelike.databinding.SheetInventoryBinding;
import io.github.matteocardarelli.simpleroguelike.snapshot.InventoryEntry;
import io.github.matteocardarelli.simpleroguelike.ui.content.ContentPresentation;
import io.github.matteocardarelli.simpleroguelike.ui.hud.PlayerBars;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * L'inventario: salute ed esperienza del player, una griglia di oggetti e, sotto, la scheda di
 * quello selezionato con il pulsante "Usa". Un tocco seleziona, non usa. Il dialog ha una finestra
 * sua che consuma indietro, quindi l'Activity non registra callback. Dopo una rotazione il sistema
 * lo ricrea da solo, e l'Activity evita di aprirne un secondo.
 *
 * <p>Conosce solo {@link InventoryHost}: le scelte le decide il ViewModel, e lo sheet mostra lo
 * stato che riceve.
 */
public final class InventorySheet extends BottomSheetDialogFragment {

    public static final String TAG = "inventory";
    private static final int COLUMNS = 4;

    private @Nullable SheetInventoryBinding binding;
    private @Nullable InventoryAdapter adapter;

    private InventoryHost host() {
        return (InventoryHost) requireActivity();
    }

    /** Il dialog vede tutti i tocchi, anche sopra la griglia: lì un doppio tap chiude lo sheet. */
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        GestureDetector gestures =
                new GestureDetector(
                        requireContext(),
                        new GestureDetector.SimpleOnGestureListener() {
                            @Override
                            public boolean onDoubleTap(MotionEvent e) {
                                host().onInventoryToggled();
                                return true;
                            }
                        });
        BottomSheetDialog dialog =
                new BottomSheetDialog(requireContext(), getTheme()) {
                    @Override
                    public boolean dispatchTouchEvent(MotionEvent event) {
                        gestures.onTouchEvent(event);
                        return super.dispatchTouchEvent(event);
                    }
                };
        dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        dialog.getBehavior().setSkipCollapsed(true);
        return dialog;
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        SheetInventoryBinding created = SheetInventoryBinding.inflate(inflater, container, false);
        InventoryAdapter created2 = new InventoryAdapter(host().content(), host()::onItemSelected);
        created.grid.setLayoutManager(new GridLayoutManager(requireContext(), COLUMNS));
        created.grid.setAdapter(created2);
        created.use.setOnClickListener(v -> host().onUseSelected());
        binding = created;
        adapter = created2;
        show(host().currentInventory());
        return created.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        adapter = null;
    }

    /**
     * Chiusura senza scelta: non fa passare il turno. Non si usa onDismiss, che arriva anche alla
     * rotazione.
     */
    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        host().onInventoryCancelled();
    }

    /** Mostra le barre del player, gli oggetti e la scheda di quello selezionato. */
    public void show(InventoryContent inventory) {
        SheetInventoryBinding binding = this.binding;
        InventoryAdapter adapter = this.adapter;
        if (binding == null || adapter == null) {
            return;
        }
        PlayerBars.showWithValues(binding.bars, inventory.player());
        adapter.show(inventory.items(), inventory.selected().orElse(null));
        ContentPresentation content = host().content();
        Optional<InventoryEntry> selected = inventory.selectedEntry();
        if (selected.isPresent()) {
            InventoryEntry entry = selected.get();
            binding.detailArt.setVisibility(View.VISIBLE);
            binding.detailArt.setBackgroundTintList(
                    ColorStateList.valueOf(
                            requireContext().getColor(content.colorRes(entry.kind()))));
            binding.detailName.setText(content.nameRes(entry.kind()));
            binding.detailDescription.setText(content.descriptionRes(entry.kind()));
            binding.use.setEnabled(true);
            binding.use.setText(
                    !entry.slot().isPresent()
                            ? R.string.inventory_use
                            : entry.equipped()
                                    ? R.string.inventory_unequip
                                    : R.string.inventory_equip);
        } else {
            binding.detailArt.setVisibility(View.INVISIBLE);
            binding.detailName.setText(
                    inventory.items().isEmpty()
                            ? R.string.inventory_empty
                            : R.string.inventory_select);
            binding.detailDescription.setText("");
            binding.use.setEnabled(false);
            binding.use.setText(R.string.inventory_use);
        }
    }
}
