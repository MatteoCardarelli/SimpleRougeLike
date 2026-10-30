package io.github.matteocardarelli.simpleroguelike.ui.inventory;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import io.github.matteocardarelli.simpleroguelike.databinding.ItemInventoryEntryBinding;
import io.github.matteocardarelli.simpleroguelike.model.EntityId;
import io.github.matteocardarelli.simpleroguelike.snapshot.InventoryEntry;
import io.github.matteocardarelli.simpleroguelike.ui.content.ContentPresentation;
import java.util.List;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;

/** Le caselle della griglia: una per oggetto, con il colore del suo {@code Kind} e il nome. */
final class InventoryAdapter extends ListAdapter<InventoryEntry, InventoryAdapter.Holder> {

    private static final DiffUtil.ItemCallback<InventoryEntry> SAME_ENTRY =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(InventoryEntry a, InventoryEntry b) {
                    return a.id().equals(b.id());
                }

                @Override
                public boolean areContentsTheSame(InventoryEntry a, InventoryEntry b) {
                    return a.equals(b);
                }
            };

    private final ContentPresentation content;
    private final Consumer<EntityId> onSelected;
    private @Nullable EntityId selected;

    InventoryAdapter(ContentPresentation content, Consumer<EntityId> onSelected) {
        super(SAME_ENTRY);
        this.content = content;
        this.onSelected = onSelected;
    }

    /** Mostra gli oggetti e ridisegna la selezione, che può cambiare a parità di oggetti. */
    void show(List<InventoryEntry> items, @Nullable EntityId selected) {
        boolean selectionChanged = !java.util.Objects.equals(this.selected, selected);
        this.selected = selected;
        submitList(items);
        if (selectionChanged) {
            notifyItemRangeChanged(0, getItemCount());
        }
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(
                ItemInventoryEntryBinding.inflate(
                        LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        InventoryEntry entry = getItem(position);
        ItemInventoryEntryBinding binding = holder.binding;
        String name = binding.getRoot().getContext().getString(content.nameRes(entry.kind()));
        binding.name.setText(name);
        binding.art.setBackgroundTintList(
                ColorStateList.valueOf(
                        binding.getRoot().getContext().getColor(content.colorRes(entry.kind()))));
        binding.getRoot().setContentDescription(name);
        binding.getRoot().setSelected(entry.id().equals(selected));
        binding.getRoot().setOnClickListener(v -> onSelected.accept(entry.id()));
    }

    static final class Holder extends RecyclerView.ViewHolder {

        final ItemInventoryEntryBinding binding;

        Holder(ItemInventoryEntryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
