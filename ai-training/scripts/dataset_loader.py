from __future__ import annotations

from pathlib import Path
from typing import Final

import tensorflow as tf


CLASS_NAMES: Final[list[str]] = [
    "prescription",
    "no_glasses",
    "sunglasses",
    "occluded",
]

SUPPORTED_EXTENSIONS: Final[set[str]] = {
    ".jpg",
    ".jpeg",
    ".png",
    ".bmp",
    ".webp",
}


def validate_dataset_directory(dataset_dir: str | Path) -> Path:
    root = Path(dataset_dir).expanduser().resolve()

    if not root.exists():
        raise FileNotFoundError(f"Dataset directory does not exist: {root}")

    if not root.is_dir():
        raise NotADirectoryError(f"Dataset path is not a directory: {root}")

    missing_classes = [
        class_name
        for class_name in CLASS_NAMES
        if not (root / class_name).is_dir()
    ]

    if missing_classes:
        raise ValueError(
            "Missing class directories: "
            + ", ".join(missing_classes)
        )

    empty_classes: list[str] = []

    for class_name in CLASS_NAMES:
        class_directory = root / class_name

        image_count = sum(
            1
            for file_path in class_directory.rglob("*")
            if file_path.is_file()
            and file_path.suffix.lower() in SUPPORTED_EXTENSIONS
        )

        if image_count == 0:
            empty_classes.append(class_name)

    if empty_classes:
        raise ValueError(
            "The following class directories contain no supported images: "
            + ", ".join(empty_classes)
        )

    return root


def create_datasets(
    dataset_dir: str | Path,
    image_width: int = 224,
    image_height: int = 96,
    batch_size: int = 32,
    validation_split: float = 0.20,
    seed: int = 1337,
) -> tuple[tf.data.Dataset, tf.data.Dataset]:
    root = validate_dataset_directory(dataset_dir)

    common_arguments = {
        "directory": str(root),
        "labels": "inferred",
        "label_mode": "categorical",
        "class_names": CLASS_NAMES,
        "image_size": (image_height, image_width),
        "batch_size": batch_size,
        "validation_split": validation_split,
        "seed": seed,
        "shuffle": True,
    }

    training_dataset = tf.keras.utils.image_dataset_from_directory(
        subset="training",
        **common_arguments,
    )

    validation_dataset = tf.keras.utils.image_dataset_from_directory(
        subset="validation",
        **common_arguments,
    )

    augmentation = tf.keras.Sequential(
        [
            tf.keras.layers.RandomFlip("horizontal"),
            tf.keras.layers.RandomRotation(0.04),
            tf.keras.layers.RandomZoom(
                height_factor=(-0.08, 0.08),
                width_factor=(-0.08, 0.08),
            ),
            tf.keras.layers.RandomTranslation(
                height_factor=0.05,
                width_factor=0.05,
            ),
            tf.keras.layers.RandomContrast(0.15),
            tf.keras.layers.RandomBrightness(0.12),
        ],
        name="training_augmentation",
    )

    autotune = tf.data.AUTOTUNE

    training_dataset = (
        training_dataset
        .map(
            lambda images, labels: (
                augmentation(images, training=True),
                labels,
            ),
            num_parallel_calls=autotune,
        )
        .prefetch(autotune)
    )

    validation_dataset = validation_dataset.prefetch(autotune)

    return training_dataset, validation_dataset


def count_images(dataset_dir: str | Path) -> dict[str, int]:
    root = validate_dataset_directory(dataset_dir)

    return {
        class_name: sum(
            1
            for file_path in (root / class_name).rglob("*")
            if file_path.is_file()
            and file_path.suffix.lower() in SUPPORTED_EXTENSIONS
        )
        for class_name in CLASS_NAMES
    }


if __name__ == "__main__":
    dataset_path = Path(__file__).resolve().parent.parent / "datasets"

    try:
        counts = count_images(dataset_path)

        print("Dataset validation succeeded.")
        print(f"Class order: {CLASS_NAMES}")

        for name, count in counts.items():
            print(f"{name}: {count}")

    except Exception as error:
        print(f"Dataset validation failed: {error}")
