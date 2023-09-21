"""
Methods for image selection
"""
import difflib
import re

from bibigrid.models.exceptions import ImageNotActiveException


def select_image(provider, image, log, fallback=None):
    # check if image is active
    active_images = provider.get_active_images()
    if image not in active_images:
        old_image = image
        log.info(f"Image '{old_image}' has no direct match. Maybe it's a regex? Trying regex match.")
        image = next((elem for elem in active_images if re.match(image, elem)), None)
        if not image:
            log.warning(f"Couldn't find image '{old_image}'.")
            if isinstance(fallback, str):
                image = next(elem for elem in active_images if re.match(fallback, elem))
                log.info(f"Taking first regex ('{fallback}') match '{image}'.")
            elif fallback:
                image = difflib.get_close_matches(old_image, active_images)[0]
                log.info(f"Taking closest active image (in name) '{image}' instead.")
            else:
                raise ImageNotActiveException(f"Image {old_image} no longer active or doesn't exist.")
            log.info(f"Using alternative '{image}' instead of '{old_image}'. You should change the configuration.")
        else:
            log.info(f"Taking first regex match: '{image}'")
    return image
